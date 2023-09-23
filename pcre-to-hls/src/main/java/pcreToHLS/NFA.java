package pcreToHLS;

import java.util.Arrays;
import java.util.Collections;
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


public class NFA extends FinalAutomaton {

    public NFA(Graph<String, DefaultEdge> graph, String start, Set<String> ends) 
    {
        this.graph = graph;
        this.start = start;
        this.ends = ends;
    }

    public NFA(ParseTree root, RulesAnalyzer analyzer, String flags) throws EmptyStackException
    {
        EpsilonNFA eNFA = new EpsilonNFA(root, analyzer, flags);
        // System.out.println("\n=== e-NFA ===");
        // eNFA.print();
        // eNFA.display();
        NFA nfa = eNFA.toRegularNFA();
        // System.out.println("\n=== NFA ===");
        // nfa.print();
        // nfa.display();
        copy(nfa);
    }

    public DFA toDFA() // powerset construction
    {
        Graph<String, DefaultEdge> new_graph = new DirectedPseudograph<>(LabeledEdge.class);
        Stack<Set<String>> outer_states = new Stack<>();
        Map<Set<String>, String> outer_state_map = new HashMap<>();
        Set<String> new_ends = new HashSet<>();
        String new_start_id = VertexIDFactory.getNewVertexID();
        outer_states.push(Set.of(this.start));
        outer_state_map.put(Set.of(this.start), new_start_id);
        new_graph.addVertex(new_start_id);

        while (!outer_states.isEmpty())
        {
            Set<String> outer_state = outer_states.pop();

            if (!Collections.disjoint(outer_state, this.ends))
                new_ends.add(outer_state_map.get(outer_state));
    
            Map<LabeledEdge<?>, Set<String>> new_sub_states = new HashMap<>();
            for (String sub_state : outer_state)
                for (DefaultEdge edge : this.graph.outgoingEdgesOf(sub_state))
                {
                    LabeledEdge<?> edge_copy = ((LabeledEdge<?>) edge).copy();
                    String edge_target = this.graph.getEdgeTarget(edge);
                    if (new_sub_states.putIfAbsent(edge_copy, new HashSet<>(Arrays.asList(edge_target))) != null)
                        new_sub_states.get(edge_copy).add(edge_target);
                }

            for (Entry<LabeledEdge<?>, Set<String>>  entry : new_sub_states.entrySet())
            {
                String source_state_id = outer_state_map.get(outer_state);
    
                String destination_state_id = "";
                if (outer_state_map.containsKey(entry.getValue()))
                    destination_state_id = outer_state_map.get(entry.getValue());
                else
                {
                    destination_state_id = VertexIDFactory.getNewVertexID();
                    outer_state_map.put(entry.getValue(), destination_state_id);
                    new_graph.addVertex(destination_state_id);
                    outer_states.push(entry.getValue());
                }

                new_graph.addEdge(source_state_id, destination_state_id, entry.getKey());
            }

        }

        return new DFA(new_graph, new_start_id, new_ends);
    }
}
