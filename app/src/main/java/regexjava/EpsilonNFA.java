package regexjava;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jgrapht.*;
import org.jgrapht.graph.*;

import regexjava.Counter.CounterOperation;


public class EpsilonNFA {

    private Graph<String, DefaultEdge> graph = new DirectedMultigraph<>(LabeledEdge.class);
    private String start;
    private String end;

    public EpsilonNFA(Graph<String, DefaultEdge> graph, String start, String end) {
        this.graph = graph;
        this.start = start;
        this.end = end;
    }

    private void copy(EpsilonNFA other)
    {
        this.graph = other.graph;
        this.start = other.start;
        this.end = other.end;
    }

    private static EpsilonNFA duplicate(EpsilonNFA other)
    {
        Graph<String, DefaultEdge> new_graph = new DirectedMultigraph<>(LabeledEdge.class);
        Map<String, String> vertex_map = new HashMap<>();
        for (String vertex : other.graph.vertexSet())
        {
            String vertex_copy = VertexIDFactory.getNewVertexID();
            vertex_map.put(vertex, vertex_copy);
            new_graph.addVertex(vertex_copy);
        }

        for (DefaultEdge edge : other.graph.edgeSet())
        {
            String source = vertex_map.get(other.graph.getEdgeSource(edge));
            String target = vertex_map.get(other.graph.getEdgeTarget(edge));
            DefaultEdge new_edge = ((LabeledEdge<?>) edge).copy();
            new_graph.addEdge(source, target, new_edge);
        }

        return new EpsilonNFA(new_graph, vertex_map.get(other.start), vertex_map.get(other.end));
    }

    private static void updateGraphEdges(Graph<String, DefaultEdge> graph, List<DefaultEdge> to_remove, List<Object[]> to_add)
    {
        for (DefaultEdge edge : to_remove)
            graph.removeEdge(edge);

        for (Object[] add_element : to_add)
            graph.addEdge((String) add_element[0], (String) add_element[1], (LabeledEdge<?>) add_element[2]);

        to_remove.clear();
        to_add.clear();
    }

    public <T> EpsilonNFA(LabeledEdge<T> edge)
    {
        if (edge.getClass() == CharacterBlockEdge.class)
            blockEdgedEpsilonNFA((CharacterBlockEdge)edge);
        else 
            singleEdgeEpsilonNFA(edge);
    }

    private <T> void singleEdgeEpsilonNFA(LabeledEdge<T> edge)
    {
        String v1 = VertexIDFactory.getNewVertexID();
        String v2 = VertexIDFactory.getNewVertexID();
        this.graph.addVertex(v1);
        this.graph.addVertex(v2);
        this.graph.addEdge(v1, v2, edge);
        this.start = v1;
        this.end = v2;
    }

    private void blockEdgedEpsilonNFA(CharacterBlockEdge block_trans)
    {
        Stack<EpsilonNFA> stack = new Stack<>();
        Integer[] code_points = block_trans.getCodePoints();

        for (Integer code_point : code_points)
        {
            stack.push(new EpsilonNFA(new CharacterEdge(code_point)));

            if (stack.size() == 2)
            {
                EpsilonNFA second = stack.pop();
                EpsilonNFA first = stack.pop();
                stack.push(EpsilonNFA.concat(first, second));
            }
        }

        copy(stack.pop());
    }

    /* Creates EpsilonNFA from a regex parseTree */
    EpsilonNFA(ParseTree tree)
    {
        RegexListener listener = new RegexListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        copy(listener.getEpsilonNFA());
    }

    public static EpsilonNFA concat(EpsilonNFA first, EpsilonNFA second) {
        EpsilonNFA res = null;
        if (Graphs.addGraph(first.graph, second.graph)) {
            res = new EpsilonNFA(first.graph, first.start, second.end);
            res.graph.addEdge(first.end, second.start, new EpsilonEdge());
        }

        return res;
    }

    public static EpsilonNFA join(EpsilonNFA first, EpsilonNFA second) {
        EpsilonNFA res = null;
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        first.graph.addVertex(new_start);
        second.graph.addVertex(new_start);
        first.graph.addEdge(new_start, first.start, new EpsilonEdge());
        second.graph.addEdge(new_start, second.start, new EpsilonEdge());

        first.graph.addVertex(new_end);
        second.graph.addVertex(new_end);
        first.graph.addEdge(first.end, new_end, new EpsilonEdge());
        second.graph.addEdge(second.end, new_end, new EpsilonEdge());

        if (Graphs.addGraph(first.graph, second.graph))
            res = new EpsilonNFA(first.graph, new_start, new_end);

        return res;
    }

    public static EpsilonNFA zeroOrMore(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonEdge());
        automata.graph.addEdge(new_start, new_end, new EpsilonEdge());

        automata.graph.addEdge(automata.end, new_end, new EpsilonEdge());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonEdge());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    public static EpsilonNFA oneOrMore(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonEdge());
        automata.graph.addEdge(automata.end, new_end, new EpsilonEdge());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonEdge());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    public static EpsilonNFA zeroOrOne(EpsilonNFA automata) {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, new_end, new EpsilonEdge());
        automata.graph.addEdge(new_start, automata.start, new EpsilonEdge());
        automata.graph.addEdge(automata.end, new_end, new EpsilonEdge());

        return new EpsilonNFA(automata.graph, new_start, new_end);
    }

    private static Graph<String, DefaultEdge> prepareBoundedQuantifierDuplicate(EpsilonNFA automata, Counter counter, String new_start, String new_mid, String new_end)
    {
        EpsilonNFA duplicated = duplicate(automata);
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, automata.graph);
        Graphs.addGraph(new_graph, duplicated.graph);

        new_graph.addVertex(new_start);
        new_graph.addVertex(new_mid);
        new_graph.addVertex(new_end);

        new_graph.addEdge(new_start, duplicated.start, new EpsilonEdge());
        new_graph.addEdge(duplicated.end, new_mid, new EpsilonEdge());
        new_graph.addEdge(new_mid, automata.start, new EpsilonEdge());

        List<DefaultEdge> to_remove = new LinkedList<>();
        List<Object[]> to_add = new LinkedList<>();

        Set<DefaultEdge> duplicated_end_incomings = duplicated.graph.incomingEdgesOf(duplicated.end);
        for (DefaultEdge end_incoming : duplicated_end_incomings)
        {
            String source = duplicated.graph.getEdgeSource(end_incoming);
            LabeledEdge<?> set_edge = ((LabeledEdge<?>) end_incoming).copy();
            set_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.SET));
            if (counter.getTarget_value() == 1)
                to_add.add(new Object[] {source, new_end, set_edge.copy()});
                
            to_add.add(new Object[] {source, new_mid, set_edge});
            to_remove.add(end_incoming);
        }

        updateGraphEdges(new_graph, to_remove, to_add);
        return new_graph;
    }

    public static EpsilonNFA repeatExactly(EpsilonNFA automata, int repetitions) 
    {
        Counter counter = new Counter(repetitions);
        String new_start = VertexIDFactory.getNewVertexID();
        String new_mid = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        Graph<String, DefaultEdge> new_graph = prepareBoundedQuantifierDuplicate(automata, counter, new_start, new_mid, new_end);
        List<DefaultEdge> to_remove = new LinkedList<>();
        List<Object[]> to_add = new LinkedList<>();

        Set<DefaultEdge> end_incomings = automata.graph.incomingEdgesOf(automata.end);
        for (DefaultEdge end_incoming : end_incomings)
        {
            String source = automata.graph.getEdgeSource(end_incoming);
            LabeledEdge<?> less_edge = ((LabeledEdge<?>) end_incoming).copy();
            LabeledEdge<?> equal_edge = ((LabeledEdge<?>) end_incoming).copy();
            less_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.COMPARE_LESS));
            equal_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.COMPARE_EQUAL));
            to_add.add(new Object[] {source, new_mid, less_edge});
            to_add.add(new Object[] {source, new_end, equal_edge});
            to_remove.add(end_incoming);
        }

        updateGraphEdges(new_graph, to_remove, to_add);
        return new EpsilonNFA(new_graph, new_start, new_end);
    }

    public static EpsilonNFA repeatAtLeast(EpsilonNFA automata, int repetitions)
    {
        Counter counter = new Counter(repetitions);
        String new_start = VertexIDFactory.getNewVertexID();
        String new_mid = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        Graph<String, DefaultEdge> new_graph = prepareBoundedQuantifierDuplicate(automata, counter, new_start, new_mid, new_end);
        List<DefaultEdge> to_remove = new LinkedList<>();
        List<Object[]> to_add = new LinkedList<>();

        Set<DefaultEdge> end_incomings = automata.graph.incomingEdgesOf(automata.end);
        for (DefaultEdge end_incoming : end_incomings)
        {
            String source = automata.graph.getEdgeSource(end_incoming);
            LabeledEdge<?> less_edge = ((LabeledEdge<?>) end_incoming).copy();
            LabeledEdge<?> equalmore_edge = ((LabeledEdge<?>) end_incoming).copy();
            less_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.COMPARE_LESS));
            equalmore_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.COMPARE_EQUALMORE));
            to_add.add(new Object[] {source, new_mid, less_edge});
            to_add.add(new Object[] {source, new_end, equalmore_edge});
            to_add.add(new Object[] {new_end, new_mid, new EpsilonEdge()});
            to_remove.add(end_incoming);
        }

        updateGraphEdges(new_graph, to_remove, to_add);
        
        return new EpsilonNFA(new_graph, new_start, new_end);
    }

    public static EpsilonNFA repeatRange(EpsilonNFA automata, int min_repetitions, int max_repetitions)
    {
        Counter counter = new Counter(min_repetitions, max_repetitions);
        String new_start = VertexIDFactory.getNewVertexID();
        String new_mid = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        Graph<String, DefaultEdge> new_graph = prepareBoundedQuantifierDuplicate(automata, counter, new_start, new_mid, new_end);
        List<DefaultEdge> to_remove = new LinkedList<>();
        List<Object[]> to_add = new LinkedList<>();

        Set<DefaultEdge> end_incomings = automata.graph.incomingEdgesOf(automata.end);
        for (DefaultEdge end_incoming : end_incomings)
        {
            String source = automata.graph.getEdgeSource(end_incoming);
            LabeledEdge<?> less_edge = ((LabeledEdge<?>) end_incoming).copy();
            LabeledEdge<?> range_edge = ((LabeledEdge<?>) end_incoming).copy();
            less_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.COMPARE_LESS));
            range_edge.setCounterInfo(new CounterInfo(counter, CounterOperation.COMPARE_RANGE));
            to_add.add(new Object[] {source, new_mid, less_edge});
            to_add.add(new Object[] {source, new_end, range_edge});
            to_add.add(new Object[] {new_end, new_mid, new EpsilonEdge()});
            to_remove.add(end_incoming);
        }

        updateGraphEdges(new_graph, to_remove, to_add);
        
        return new EpsilonNFA(new_graph, new_start, new_end);
    }

    private static void getEpsilonClosure(Graph<String, DefaultEdge> graph, String vertex, Set<String> closure) {
        closure.add(vertex);
        for (DefaultEdge edge : graph.outgoingEdgesOf(vertex)) {
            if (edge.getClass() == EpsilonEdge.class) {
                String out_vertex = graph.getEdgeTarget(edge);
                getEpsilonClosure(graph, out_vertex, closure);
            }
        }
    }

    private static void removeDeadStates(Graph<String, DefaultEdge> graph, Set<String> starts, Set<String> ends)
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
                    if (edge.getClass() == EpsilonEdge.class)
                        continue;

                    String joinable = graph.getEdgeTarget(edge);
                    LabeledEdge<?> new_edge = ((LabeledEdge<?>) edge).copy();
                    new_graph.addEdge(vertex, joinable, new_edge);
                }
            }
        }

        for (DefaultEdge edge : graph.edgeSet()) {
            if (edge.getClass() == EpsilonEdge.class)
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
