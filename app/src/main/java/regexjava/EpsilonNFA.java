package regexjava;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jgrapht.*;
import org.jgrapht.graph.*;


public class EpsilonNFA {

    private Graph<String, DefaultEdge> graph = new DirectedMultigraph<>(LabeledEdge.class);
    private String start;
    private String end;

    public EpsilonNFA(Graph<String, DefaultEdge> graph, String start, String end) {
        this.graph = graph;
        this.start = start;
        this.end = end;
    }

    /* Creates Empty/Wildcard Transition EpsilonNFA */
    public <T> EpsilonNFA(Class<T> transition_type) {
        String v1 = VertexIDFactory.getNewVertexID();
        String v2 = VertexIDFactory.getNewVertexID();
        this.graph.addVertex(v1);
        this.graph.addVertex(v2);
        DefaultEdge new_edge;
        if (transition_type == WildcardTransition.class)
            new_edge = new WildcardTransition();
        else
            new_edge = new EpsilonTransition();

        this.graph.addEdge(v1, v2, new_edge);
        this.start = v1;
        this.end = v2;
    }

    /* Creates Single Transition EpsilonNFA */
    public EpsilonNFA(char symbol) {
        String v1 = VertexIDFactory.getNewVertexID();
        String v2 = VertexIDFactory.getNewVertexID();
        this.graph.addVertex(v1);
        this.graph.addVertex(v2);
        this.graph.addEdge(v1, v2, new RegularTransition(symbol));
        this.start = v1;
        this.end = v2;
    }

    /* Creates EpsilonNFA from a regex parseTree */
    public EpsilonNFA(ParseTree tree)
    {
        RegexListener listener = new RegexListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        EpsilonNFA nfa = listener.getEpsilonNFA();
        this.start = nfa.start;
        this.end = nfa.end;
        this.graph = nfa.graph;
    }

    public static EpsilonNFA concat(EpsilonNFA first, EpsilonNFA second) {
        EpsilonNFA res = null;
        if (Graphs.addGraph(first.graph, second.graph)) {
            res = new EpsilonNFA(first.graph, first.start, second.end);
            res.graph.addEdge(first.end, second.start, new EpsilonTransition());
        }

        return res;
    }

    public static EpsilonNFA join(EpsilonNFA first, EpsilonNFA second) {
        EpsilonNFA res = null;
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        first.graph.addVertex(new_start);
        second.graph.addVertex(new_start);
        first.graph.addEdge(new_start, first.start, new EpsilonTransition());
        second.graph.addEdge(new_start, second.start, new EpsilonTransition());

        first.graph.addVertex(new_end);
        second.graph.addVertex(new_end);
        first.graph.addEdge(first.end, new_end, new EpsilonTransition());
        second.graph.addEdge(second.end, new_end, new EpsilonTransition());

        if (Graphs.addGraph(first.graph, second.graph))
            res = new EpsilonNFA(first.graph, new_start, new_end);

        return res;
    }

    public static EpsilonNFA zeroOrMore(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(new_start, new_end, new EpsilonTransition());

        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonTransition());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    public static EpsilonNFA oneOrMore(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonTransition());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    public static EpsilonNFA zeroOrOne(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, new_end, new EpsilonTransition());
        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    private static void getEpsilonClosure(Graph<String, DefaultEdge> graph, String vertex, Set<String> closure) {
        closure.add(vertex);
        for (DefaultEdge edge : graph.outgoingEdgesOf(vertex)) {
            if (edge.getClass() == EpsilonTransition.class) {
                String out_vertex = graph.getEdgeTarget(edge);
                getEpsilonClosure(graph, out_vertex, closure);
            }
        }
    }

    public static void removeDeadStates(Graph<String, DefaultEdge> graph, Set<String> starts, Set<String> ends)
    {
        Set<String> to_remove = new HashSet<>();
        for (String vertex : graph.vertexSet()) 
        {
            if (starts.contains(vertex))
                continue;

            Set<DefaultEdge> incoming = graph.incomingEdgesOf(vertex);
            if (incoming.isEmpty())
                to_remove.add(vertex);
        }

        for (String vertex : to_remove)
        {
            graph.removeVertex(vertex);
            ends.remove(vertex);
        }
    }

    public NFA toRegularNFA() 
    {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);
        Set<String> new_ends = new HashSet<>();
        Set<String> vertices = graph.vertexSet();

        for (String vertex : vertices) 
        {
            Set<String> closure = new HashSet<>();
            getEpsilonClosure(graph, vertex, closure);
            if (closure.contains(this.end))
                new_ends.add(vertex);

            for (String reachable : closure) 
            {
                Set<DefaultEdge> edges = graph.outgoingEdgesOf(reachable);
                for (DefaultEdge edge : edges) 
                {
                    if (edge.getClass() == EpsilonTransition.class)
                        continue;

                    
                    String joinable = graph.getEdgeTarget(edge);
                    DefaultEdge new_edge;
                    if (edge.getClass() == WildcardTransition.class)
                        new_edge = new WildcardTransition();
                    else 
                    {
                        char symbol = ((RegularTransition) edge).getSymbol();
                        new_edge =  new RegularTransition(symbol);
                    }
                    new_graph.addEdge(vertex, joinable, new_edge);
                }
            }
        }

        for (DefaultEdge edge : graph.edgeSet()) {
            if (edge.getClass() == EpsilonTransition.class)
                new_graph.removeEdge(edge);
        }

        removeDeadStates(new_graph, new HashSet<>(Arrays.asList(this.start)), new_ends);
        return new NFA(new_graph, this.start, new_ends);
    }

    public void print()
    {
        System.out.println("Start: " + this.start);
        System.out.println("Ends: " + this.end);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
