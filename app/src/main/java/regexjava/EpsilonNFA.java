package regexjava;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

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

    /* Creates Empty Transition EpsilonNFA */
    public EpsilonNFA() {
        String v1 = VertexIDFactory.getNewVertexID();
        String v2 = VertexIDFactory.getNewVertexID();
        this.graph.addVertex(v1);
        this.graph.addVertex(v2);
        this.graph.addEdge(v1, v2, new EpsilonTransition());
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

    /* Creates EpsilonNFA from a postfix expression [ such as 'ab|' for '(a|b)' ] */
    public EpsilonNFA(String postfix_expr) {
        Stack<EpsilonNFA> stack = new Stack<>();

        for (final char token : postfix_expr.toCharArray()) {
            if (token == '.') {
                EpsilonNFA second = stack.pop();
                EpsilonNFA first = stack.pop();
                stack.push(concat(first, second));
            } else if (token == '|') {
                EpsilonNFA second = stack.pop();
                EpsilonNFA first = stack.pop();
                stack.push(join(first, second));
            } else if (token == '*') {
                EpsilonNFA top = stack.pop();
                stack.push(closure(top));
            } else if (token == '+') {
                EpsilonNFA top = stack.pop();
                stack.push(oneOrMore(top));
            } else if (token == '?') {
                EpsilonNFA top = stack.pop();
                stack.push(zeroOrOne(top));
            } else
                stack.push(new EpsilonNFA(token));
        }

        EpsilonNFA top = stack.peek();
        this.start = top.start;
        this.end = top.end;
        this.graph = top.graph;
    }

    private EpsilonNFA concat(EpsilonNFA first, EpsilonNFA second) {
        EpsilonNFA res = null;
        if (Graphs.addGraph(first.graph, second.graph)) {
            res = new EpsilonNFA(first.graph, first.start, second.end);
            res.graph.addEdge(first.end, second.start, new EpsilonTransition());
        }

        return res;
    }

    private EpsilonNFA join(EpsilonNFA first, EpsilonNFA second) {
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

    private EpsilonNFA closure(EpsilonNFA automata) {
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

    private EpsilonNFA oneOrMore(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonTransition());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    private EpsilonNFA zeroOrOne(EpsilonNFA automata) {
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
                    char symbol = ((RegularTransition) edge).getSymbol();
                    RegularTransition new_edge =  new RegularTransition(symbol);
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


    // ============== CODE BELOW NOT 100% =============
    // VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV

    // private void addCorrectEdgeType(Graph<String, DefaultEdge> new_graph, String source, String dest, char symbol)
    // {
    //     DefaultEdge new_edge;
    //     if (symbol == 0)
    //         new_edge = new EpsilonTransition();
    //     else 
    //         new_edge = new RegularTransition(symbol);
        
    //     new_graph.addEdge(source, dest, new_edge);
    // }

    // private void bypassEpsilon(String base, String next, char base_crit, Graph<String, DefaultEdge> new_graph, Set<String> visited)
    // {
    //     if (visited.contains(next))
    //     {
    //         addCorrectEdgeType(new_graph, base, next, base_crit);
    //         return;
    //     }
    //     visited.add(next);
    //     for (DefaultEdge e : graph.outgoingEdgesOf(next)) 
    //     {
    //         String k = graph.getEdgeTarget(e);
    //         if (e.getClass() == EpsilonTransition.class)
    //             bypassEpsilon(base, k, base_crit, new_graph, visited);
    //         else 
    //             addCorrectEdgeType(new_graph, base, next, base_crit);
    //     }
    //     visited.remove(next);
    //     return;
    // }

    // public NFA toRegularNFA()
    // {
    //     Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
    //     Graphs.addGraph(new_graph, this.graph);

    //     Set<String> visited = new HashSet<>();
    //     for(String i : graph.vertexSet())
    //         for(DefaultEdge e : graph.outgoingEdgesOf(i))
    //         {
    //             String j = graph.getEdgeTarget(e);
    //             char symbol = 0;
    //             if (e.getClass() == RegularTransition.class)
    //                 symbol = ((RegularTransition)e).getSymbol();

    //             bypassEpsilon(i, j, symbol, new_graph, visited);
    //         }
        
    //     for (DefaultEdge edge : graph.edgeSet()) 
    //     {
    //         if (edge.getClass() == EpsilonTransition.class)
    //             new_graph.removeEdge(edge);
    //     }

    //     return new NFA(new_graph, start, new HashSet<>(Arrays.asList(end)));
    // }
}
