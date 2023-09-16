package pcreToHLS;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import org.antlr.v4.runtime.tree.ParseTree;
import org.jgrapht.*;
import org.jgrapht.graph.*;


public class NFA {
    private String start;
    private Set<String> ends;
    private Graph<String, DefaultEdge> graph = new DirectedPseudograph<>(LabeledEdge.class);

    public NFA(Graph<String, DefaultEdge> graph, String start, Set<String> ends) 
    {
        this.graph = graph;
        this.start = start;
        this.ends = ends;
    }

    public NFA(ParseTree root, RulesAnalyzer analyzer) throws EmptyStackException
    {
        EpsilonNFA eNFA = new EpsilonNFA(root, analyzer);
        // System.out.println("=== e-NFA ===");
        // eNFA.print();
        NFA nfa = eNFA.toRegularNFA();
        System.out.println("\n=== NFA ===");
        nfa.print();
        this.graph = nfa.graph;
        this.start = nfa.start;
        this.ends = nfa.ends;
    }

    public String getStart() 
    {
        return start;
    }

    public Set<String> getEnds() 
    {
        return ends;
    }

    public Graph<String, DefaultEdge> getGraph() 
    {
        return graph;
    }

    public DFA toDFA()
    {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Stack<Set<String>> outer_states = new Stack<>();
        Map<Set<String>, String> outer_state_map = new HashMap<>();
        outer_states.push(Set.of(this.start));

        while (!outer_states.isEmpty())
        {
            Set<String> outer_state = outer_states.pop();
            if (!outer_state_map.containsKey(outer_state))
            {
                String new_id = VertexIDFactory.getNewVertexID();
                outer_state_map.put(outer_state, new_id);
                new_graph.addVertex(new_id);
            }

            for (String sub_state : outer_state)
            {
                Map<Object, Entry<Set<String>, LabeledEdge<?>>> new_sub_states = new HashMap<>();
                for (DefaultEdge edge : this.graph.outgoingEdgesOf(sub_state))
                {
                    Object edge_label = ((LabeledEdge<?>) edge).label;
                    String edge_target = this.graph.getEdgeTarget(edge);
                    if (new_sub_states.putIfAbsent(edge_label, new AbstractMap.SimpleEntry<>(new HashSet<>(Arrays.asList(edge_target)), (LabeledEdge<?>) edge)) != null)
                        new_sub_states.get(edge_label).getKey().add(edge_target);
                }

                for (Entry<Set<String>, LabeledEdge<?>>  entry : new_sub_states.values())
                {
                    String source_state_id = outer_state_map.get(outer_state);
       
                    String destination_state_id = "";
                    if (outer_state_map.containsKey(entry.getKey()))
                        destination_state_id = outer_state_map.get(entry.getKey());
                    else
                    {
                        destination_state_id = VertexIDFactory.getNewVertexID();
                        outer_state_map.put(entry.getKey(), destination_state_id);
                        new_graph.addVertex(destination_state_id);
                        outer_states.push(entry.getKey());
                    }

                    new_graph.addEdge(source_state_id, destination_state_id, entry.getValue().copy());
                }
            }

        }

        return new DFA(new_graph, "", null);
    }


    public void print()
    {
        System.out.println("Start: " + this.start);
        System.out.println("Ends: " + this.ends);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
