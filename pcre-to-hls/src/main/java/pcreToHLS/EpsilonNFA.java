package pcreToHLS;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.jgrapht.*;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import pcreToHLS.CaptureEdge.CaptureType;
import pcreToHLS.Counter.CounterOperation;

public class EpsilonNFA {

    private Graph<String, DefaultEdge> graph = new DirectedMultigraph<>(LabeledEdge.class);
    private String start;
    private String end;
    private Set<Integer> unused_fifos = new HashSet<>();
    private Set<Integer> used_fixed_fifos = new HashSet<>();

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
    EpsilonNFA(ParseTree tree, String flags) throws EmptyStackException
    {
        RegexListener listener = new RegexListener(flags);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        copy(listener.getEpsilonNFA());
        this.unused_fifos = listener.getUnusedFifos();
        this.used_fixed_fifos = listener.getUsedFixedFifos();
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

    private static Graph<String, DefaultEdge> prepareBoundedQuantifierDuplicate(EpsilonNFA automata, Counter counter, String new_start, String new_mid, String new_end, boolean read_on_zero)
    {
        EpsilonNFA duplicated = duplicate(automata);
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, automata.graph);
        Graphs.addGraph(new_graph, duplicated.graph);

        new_graph.addVertex(new_start);
        new_graph.addVertex(new_mid);
        new_graph.addVertex(new_end);

        new_graph.addEdge(new_start, duplicated.start, new EpsilonEdge());
        new_graph.addEdge(new_mid, automata.start, new EpsilonEdge());

        if (counter.getTarget_value1() == 1)
            new_graph.addEdge(duplicated.end, new_end, new CounterEdge(new CounterInfo(counter, CounterOperation.SET)));

        if (counter.getTarget_value1() == 0)
        {
            new_graph.addEdge(new_start, new_end, new EpsilonEdge());
            if (read_on_zero)
                new_graph.addEdge(duplicated.end, new_end, new CounterEdge(new CounterInfo(counter, CounterOperation.SET)));
        }


        new_graph.addEdge(duplicated.end, new_mid, new CounterEdge(new CounterInfo(counter, CounterOperation.SET)));

        return new_graph;
    }

    public static EpsilonNFA repeatExactly(EpsilonNFA automata, int repetitions) 
    {
        Counter counter = new Counter(repetitions);
        String new_start = VertexIDFactory.getNewVertexID();
        String new_mid = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        Graph<String, DefaultEdge> new_graph = prepareBoundedQuantifierDuplicate(automata, counter, new_start, new_mid, new_end, false);

        new_graph.addEdge(automata.end, new_mid, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_LESS)));
        new_graph.addEdge(automata.end, new_end, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_EQUAL)));

        return new EpsilonNFA(new_graph, new_start, new_end);
    }

    public static EpsilonNFA repeatAtLeast(EpsilonNFA automata, int repetitions)
    {
        Counter counter = new Counter(repetitions);
        String new_start = VertexIDFactory.getNewVertexID();
        String new_mid = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        Graph<String, DefaultEdge> new_graph = prepareBoundedQuantifierDuplicate(automata, counter, new_start, new_mid, new_end, true);

        new_graph.addEdge(automata.end, new_mid, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_LESS)));
        new_graph.addEdge(automata.end, new_mid, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_EQUALMORE)));
        new_graph.addEdge(automata.end, new_end, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_EQUALMORE)));
        
        return new EpsilonNFA(new_graph, new_start, new_end);
    }

    public static EpsilonNFA repeatRange(EpsilonNFA automata, int min_repetitions, int max_repetitions)
    {
        Counter counter = new Counter(min_repetitions, max_repetitions);
        String new_start = VertexIDFactory.getNewVertexID();
        String new_mid = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        Graph<String, DefaultEdge> new_graph = prepareBoundedQuantifierDuplicate(automata, counter, new_start, new_mid, new_end, true);

        new_graph.addEdge(automata.end, new_mid, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_LESS)));
        new_graph.addEdge(automata.end, new_mid, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_RANGE)));
        new_graph.addEdge(automata.end, new_end, new CounterEdge(new CounterInfo(counter, CounterOperation.COMPARE_RANGE)));
        
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

    private static void getStartAnchorClosure(Graph<String, DefaultEdge> graph, String vertex, Set<String> closure) {
        for (DefaultEdge edge : graph.outgoingEdgesOf(vertex)) {
            if (edge.getClass() == StartAnchorEdge.class) {
                String out_vertex = graph.getEdgeTarget(edge);
                closure.add(out_vertex);
                getStartAnchorClosure(graph, out_vertex, closure);
            }
        }
    }

    private static void getEndAnchorClosure(Graph<String, DefaultEdge> graph, String vertex, Set<String> closure) {
        for (DefaultEdge edge : graph.outgoingEdgesOf(vertex)) {
            if (edge.getClass() == EndAnchorEdge.class) {
                String out_vertex = graph.getEdgeTarget(edge);
                closure.add(out_vertex);
                getEndAnchorClosure(graph, out_vertex, closure);
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
            else
            {
                boolean only_self_edges = true;
                for (DefaultEdge incoming_edge : incoming)
                {
                    if (!graph.getEdgeSource(incoming_edge).equals(graph.getEdgeTarget(incoming_edge)))
                    {
                        only_self_edges = false;
                        break;
                    }   
                }
                if (only_self_edges)
                    to_remove.add(vertex);
            }
        }

        for (String vertex : to_remove)
        {
            graph.removeVertex(vertex);
            ends.remove(vertex);
        }
    }

    private Set<String> removeEpsilons()
    {
        return this.removeEpsilons(new HashSet<>(Arrays.asList(this.end)));
    }

    private Set<String> removeEpsilons(Set<String> current_ends)
    {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);
        Set<String> new_ends = new HashSet<>();
        for (String vertex : this.graph.vertexSet()) 
        {
            Set<String> closure = new HashSet<>();
            getEpsilonClosure(this.graph, vertex, closure);
            Set<String> intersection = new HashSet<>(closure);
            intersection.retainAll(current_ends);

            if (!intersection.isEmpty())
                new_ends.add(vertex);

            for (String reachable : closure) 
            {
                if (reachable.equals(vertex))
                    continue;

                Set<DefaultEdge> edges = this.graph.outgoingEdgesOf(reachable);
                for (DefaultEdge edge : edges) {
                    if (edge.getClass() == EpsilonEdge.class)
                        continue;

                    String joinable = graph.getEdgeTarget(edge);
                    LabeledEdge<?> new_edge = ((LabeledEdge<?>) edge).copy();
                    if (!new_graph.addEdge(vertex, joinable, new_edge))
                    {
                        new_graph.removeEdge(new_edge);
                        new_graph.addEdge(vertex, joinable, new_edge);
                    }
                }
            }
        }

        for (DefaultEdge edge : graph.edgeSet()) 
            if (edge.getClass() == EpsilonEdge.class)
                new_graph.removeEdge(edge);

        this.graph = new_graph;
        return new_ends;
    }

    private void removeCounterEdges()
    {
        GraphIterator<String, DefaultEdge> iterator = new DepthFirstIterator<String, DefaultEdge>(this.graph, this.start);
        while (iterator.hasNext()) 
        {
            String current_vertex = iterator.next();
            List<DefaultEdge> to_remove = new LinkedList<>();
            List<Object[]> to_add = new LinkedList<>();
            for (DefaultEdge outgoing_edge : this.graph.outgoingEdgesOf(current_vertex))
            {
                if (outgoing_edge.getClass() != CounterEdge.class)
                    continue;

                String counter_target = this.graph.getEdgeTarget(outgoing_edge);
                for (DefaultEdge incoming_to_counter : this.graph.incomingEdgesOf(current_vertex))
                {
                    String transfer_source = this.graph.getEdgeSource(incoming_to_counter);
                    LabeledEdge<?> transfer_edge = ((LabeledEdge<?>) incoming_to_counter).copy();
                    transfer_edge.addCounterInfos(((LabeledEdge<?>) outgoing_edge).getCounterInfos());
                    to_add.add(new Object[] {transfer_source, counter_target, transfer_edge});
                    // if (this.graph.outgoingEdgesOf(this.graph.getEdgeTarget(incoming_to_counter)).size() == 1)
                    //     to_remove.add(incoming_to_counter);
                }

                to_remove.add(outgoing_edge);
            }

            this.graph.removeAllEdges(to_remove);
            for (Object[] arr : to_add)
                this.graph.addEdge((String) arr[0], (String) arr[1], (DefaultEdge) arr[2]);
        }

    }

    public void removeAnchorEdges(boolean multiline)
    {
        this.removeStartAnchors(multiline);
        this.removeEndAnchors(multiline);
    }

    public void removeEndAnchors(boolean multiline)
    {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);
        for (String current_vertex : this.graph.vertexSet()) 
        {
            Set<String> closure = new HashSet<>();
            getEndAnchorClosure(graph, current_vertex, closure);

            for (String reachable : closure) 
            {
                Set<DefaultEdge> edges = graph.incomingEdgesOf(current_vertex);
                for (DefaultEdge edge : edges) 
                {
                    if (edge.getClass() == EndAnchorEdge.class)
                        continue;

                    String joinable = graph.getEdgeSource(edge);
                    LabeledEdge<?> new_edge = ((LabeledEdge<?>) edge).copy();
                    new_edge.setAtEnd(true);
                    new_graph.addEdge(joinable, reachable, new_edge);
                }
                if (!multiline)
                    new_graph.removeAllEdges(graph.outgoingEdgesOf(reachable));
            }
        }                

        for (DefaultEdge edge : graph.edgeSet()) 
            if (edge.getClass() == EndAnchorEdge.class)
                new_graph.removeEdge(edge);

        this.graph = new_graph;
    }

    public void removeStartAnchors(boolean multiline)
    {        
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);
        for (String current_vertex : this.graph.vertexSet()) 
        {
            Set<String> closure = new HashSet<>();
            getStartAnchorClosure(graph, current_vertex, closure);

            for (String reachable : closure) 
            {
                Set<DefaultEdge> edges = graph.outgoingEdgesOf(reachable);
                for (DefaultEdge edge : edges) 
                {
                    if (edge.getClass() == StartAnchorEdge.class)
                        continue;

                    String joinable = graph.getEdgeTarget(edge);
                    LabeledEdge<?> new_edge = ((LabeledEdge<?>) edge).copy();
                    new_edge.setAtStart(true);
                    new_graph.addEdge(current_vertex, joinable, new_edge);
                }
                if (!multiline)
                    new_graph.removeAllEdges(graph.incomingEdgesOf(reachable));
            }
        }                

        for (DefaultEdge edge : graph.edgeSet()) 
            if (edge.getClass() == StartAnchorEdge.class)
                new_graph.removeEdge(edge);

        this.graph = new_graph;
    }

    private boolean isFinalEdge(DefaultEdge edge)
    {
        List<Class<?>>not_final = Arrays.asList(EpsilonEdge.class, CaptureEdge.class, StartAnchorEdge.class, EndAnchorEdge.class, CounterEdge.class);
        return !not_final.contains(edge.getClass());
    }

    private List<CaptureEdge> setCaptureMaps(Map<Fifo, Set<String>> capture_starts, Map<Fifo, Set<String>> capture_ends)
    {
        List<CaptureEdge> capture_edges = new LinkedList<>();
        for (DefaultEdge edge : this.graph.edgeSet())
        {
            if (edge.getClass() != CaptureEdge.class)
                    continue;

            CaptureEdge capture_edge = (CaptureEdge) edge;
            capture_edges.add(capture_edge);
            Fifo current_fifo = capture_edge.getFifo();
            Map<Fifo, Set<String>> capture_map = capture_edge.getType() == CaptureType.START ? capture_starts : capture_ends;
            String capture_vertex = capture_edge.getType() == CaptureType.START ? this.graph.getEdgeTarget(capture_edge) : this.graph.getEdgeSource(capture_edge);
            Set<String> capture_set = capture_map.get(current_fifo); 
            
            if (capture_set == null)
                capture_set = new HashSet<>();
            
            capture_set.add(capture_vertex);
            capture_map.put(current_fifo, capture_set);
        }

        return capture_edges;
    }

    private Set<String> propagateFifos(Set<String> current_ends)
    {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);
        Map<Fifo, Set<String>> capture_starts = new HashMap<>();
        Map<Fifo, Set<String>> capture_ends = new HashMap<>();
        List<CaptureEdge> capture_edges = this.setCaptureMaps(capture_starts, capture_ends);

        for (CaptureEdge capture_edge : capture_edges)
        {
            new_graph.removeEdge(capture_edge);
            new_graph.addEdge(this.graph.getEdgeSource(capture_edge), this.graph.getEdgeTarget(capture_edge), new EpsilonEdge());
        }

        for (Entry<Fifo, Set<String>> start_entries : capture_starts.entrySet())
        {
            Set<String> path_start_vertices = start_entries.getValue();
            Set<String> path_end_vertices = capture_ends.get(start_entries.getKey());
            AllDirectedPaths<String, DefaultEdge> all_paths = new AllDirectedPaths<>(this.graph);
            List<GraphPath<String, DefaultEdge>> paths = all_paths.getAllPaths(path_start_vertices, path_end_vertices, true, Integer.MAX_VALUE);

            for (GraphPath<String, DefaultEdge> path : paths) 
            {
                int i = 0;
                boolean increment = false;

                for (String path_vertex : path.getVertexList()) 
                {
                    if (increment)
                        i++;

                    for (DefaultEdge outgoing : this.graph.outgoingEdgesOf(path_vertex)) 
                    {
                        if (!isFinalEdge(outgoing))
                            continue;
                        else
                            increment = true;

                        Fifo fifo = start_entries.getKey();
                        boolean clear = i == 0;
                        FifoInfo fifo_info = new FifoInfo(fifo, clear);
                        ((LabeledEdge<?>) outgoing).addFifosInfo(fifo_info);
                    }
                }
            }

        }

        this.graph = new_graph;
        return this.removeEpsilons(current_ends);
    }

   private static void removeDeadends(Graph<String, DefaultEdge> graph, Set<String> starts, Set<String> ends)
   {
        AllDirectedPaths<String, DefaultEdge> all_paths = new AllDirectedPaths<>(graph);
        List<GraphPath<String, DefaultEdge>> paths = all_paths.getAllPaths(starts, ends, true, Integer.MAX_VALUE);
        Set<String> meaningful_vertices = new HashSet<>();

        for (GraphPath<String, DefaultEdge> path : paths)
            meaningful_vertices.addAll(path.getVertexList());
        
        Set<String> non_meaningful_vertices = new HashSet<>(graph.vertexSet());
            non_meaningful_vertices.removeAll(meaningful_vertices);
        
        for (String vertex : non_meaningful_vertices)
            graph.removeVertex(vertex);
   }

   private void removeUnusedFifos()
   {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);

        for (DefaultEdge edge : this.graph.edgeSet())
        {
            if (edge.getClass() != CaptureEdge.class)
                continue;

            int fifo_no = ((CaptureEdge)edge).getFifo().getId_no();
            if (this.unused_fifos.contains(fifo_no))
            {
                String edge_source = this.graph.getEdgeSource(edge);
                String edge_target = this.graph.getEdgeTarget(edge);
                new_graph.removeEdge(edge);
                new_graph.addEdge(edge_source, edge_target, new EpsilonEdge());
            }
        }

        this.graph = new_graph;
   }

   private void spliceFixedReferences()
   {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Graphs.addGraph(new_graph, this.graph);

        Map<Fifo, Set<String>> capture_starts = new HashMap<>();
        Map<Fifo, Set<String>> capture_ends = new HashMap<>();
        List<CaptureEdge> capture_edges = this.setCaptureMaps(capture_starts, capture_ends);

        for (CaptureEdge capture_edge : capture_edges)
        {
            if (this.used_fixed_fifos.contains(capture_edge.getFifo().getId_no()))
            {
                new_graph.removeEdge(capture_edge);
                new_graph.addEdge(this.graph.getEdgeSource(capture_edge), this.graph.getEdgeTarget(capture_edge), new EpsilonEdge());
            }
        }

        Map<Fifo, Graph<String, DefaultEdge>> graph_map = new HashMap<>();
        Map<Fifo, String> starts_map = new HashMap<>();
        Map<Fifo, String> ends_map = new HashMap<>();
        for (Entry<Fifo, Set<String>> start_entries : capture_starts.entrySet())
        {
            if (!this.used_fixed_fifos.contains(start_entries.getKey().getId_no()))
                continue;

            Set<String> path_start_vertices = start_entries.getValue();
            Set<String> path_end_vertices = capture_ends.get(start_entries.getKey());
            AllDirectedPaths<String, DefaultEdge> all_paths = new AllDirectedPaths<>(this.graph);
            GraphPath<String, DefaultEdge> path = all_paths.getAllPaths(path_start_vertices, path_end_vertices, true, Integer.MAX_VALUE).get(0);
            Graph<String, DefaultEdge> capture_graph = new DirectedPseudograph<>(LabeledEdge.class);

      
            for (int i = 0; i < path.getVertexList().size() - 1; i++)
            {
                for (DefaultEdge edge : this.graph.outgoingEdgesOf(path.getVertexList().get(i)))
                {
                    String source = this.graph.getEdgeSource(edge);
                    String target = this.graph.getEdgeTarget(edge);
                    if (!capture_graph.containsVertex(source))
                        capture_graph.addVertex(source);
                    if (!capture_graph.containsVertex(target))
                        capture_graph.addVertex(target);
                    DefaultEdge edge_copy = ((LabeledEdge<?>)edge).copy();
                    capture_graph.addEdge(source, target, edge_copy);
                }
            }

            starts_map.put(start_entries.getKey(), path.getStartVertex());
            ends_map.put(start_entries.getKey(), path.getEndVertex());
            graph_map.put(start_entries.getKey(), capture_graph);
        }

        for (DefaultEdge edge : this.graph.edgeSet())
        {
            if (edge.getClass() != BackreferenceEdge.class)
                continue;

            Fifo referenced_fifo = ((BackreferenceEdge)edge).getFifo();
            if (!graph_map.containsKey(referenced_fifo))
                continue;

            Map<String, String> vertex_map = new HashMap<>();
            Graph<String, DefaultEdge> capture_graph_copy = new DirectedPseudograph<>(LabeledEdge.class);

            for (DefaultEdge subgraph_edge : graph_map.get(referenced_fifo).edgeSet())
            {
                String source = this.graph.getEdgeSource(subgraph_edge);
                String target = this.graph.getEdgeTarget(subgraph_edge);
                if (!vertex_map.containsKey(source))
                {
                    String new_vertex_name = VertexIDFactory.getNewVertexID();
                    capture_graph_copy.addVertex(new_vertex_name);
                    vertex_map.put(source, new_vertex_name);
                }

                if (!vertex_map.containsKey(target))
                {
                    String new_vertex_name = VertexIDFactory.getNewVertexID();
                    capture_graph_copy.addVertex(new_vertex_name);
                    vertex_map.put(target, new_vertex_name);
                }

                DefaultEdge subgraph_edge_copy = ((LabeledEdge<?>)subgraph_edge).copy(); 
                capture_graph_copy.addEdge(vertex_map.get(source), vertex_map.get(target), subgraph_edge_copy);
            }

            Graphs.addGraph(new_graph, capture_graph_copy);
            new_graph.removeEdge(edge);
            new_graph.addEdge(new_graph.getEdgeSource(edge), vertex_map.get(starts_map.get(referenced_fifo)), new EpsilonEdge());
            new_graph.addEdge(vertex_map.get(ends_map.get(referenced_fifo)), new_graph.getEdgeTarget(edge), new EpsilonEdge());

        }

        this.graph = new_graph;
   }

    public NFA toRegularNFA(boolean multiline, boolean remove_unused, boolean expand_fixed) 
    {
        if (remove_unused)
            removeUnusedFifos();
    
        if (expand_fixed)
            spliceFixedReferences();
            
        Set<String> new_ends = this.removeEpsilons();
        removeDeadStates(this.graph, new HashSet<>(Arrays.asList(this.start)), new_ends);
        new_ends = propagateFifos(new_ends);
        removeDeadStates(this.graph, new HashSet<>(Arrays.asList(this.start)), new_ends);
        removeCounterEdges();
        removeAnchorEdges(multiline);
        removeDeadStates(this.graph, new HashSet<>(Arrays.asList(this.start)), new_ends);
        removeDeadends(this.graph, new HashSet<>(Arrays.asList(this.start)), new_ends);
        return new NFA(this.graph, this.start, new_ends);
    }

    public void display()
    {
        System.setProperty("org.graphstream.ui", "swing");
        String style = 
            "graph { fill-color: white; } node { size: 0.1gu; fill-color: rgb(0,100,255); text-style: bold; text-size: 22;} " 
            + "node.start { fill-color: rgb(0,255,100); } node.end { fill-color: rgb(255,100,0); }"
            + "node.both { fill-color: rgb(192,255,00); }"  
            + "edge { text-alignment: above; text-alignment: center; text-size: 13; text-background-mode: rounded-box; text-background-color: rgb(0,228,200); }";
        org.graphstream.graph.Graph display_graph = new MultiGraph("Automaton Graph");
        display_graph.setAttribute("ui.stylesheet", style);

        for (String vertex : this.graph.vertexSet())
        {
            Node new_node = display_graph.addNode(vertex);
            new_node.setAttribute("ui.label", vertex);
            if (this.start.equals(vertex))
                new_node.setAttribute("ui.class", "start");
            
            if (this.end.equals(vertex))
            {
                if (new_node.hasAttribute("ui.class"))
                    new_node.setAttribute("ui.class", "both");
                else 
                    new_node.setAttribute("ui.class", "end");
            }

        }
        
        for (DefaultEdge edge : this.graph.edgeSet())
        {
            String source = this.graph.getEdgeSource(edge);
            String target = this.graph.getEdgeTarget(edge);
            Edge new_edge = display_graph.addEdge(source + target + edge, source, target, true);
            new_edge.setAttribute("ui.label", edge);
        }

        display_graph.display();
    }

    public void print()
    {
        System.out.println("Start: " + this.start);
        System.out.println("Ends: " + this.end);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
