package regexjava;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class NFA {

    String start;
    Set<String> ends;
    Graph<String, DefaultEdge> graph = new DirectedPseudograph<>(LabeledEdge.class);

    public NFA(Graph<String, DefaultEdge> graph, String start, Set<String> ends) 
    {
        this.graph = graph;
        this.start = start;
        this.ends = ends;
    }

    public NFA(String raw_regex)
    {
        String prepped_expr = RegexPrepper.prep(raw_regex);
        EpsilonNFA eNFA = new EpsilonNFA(prepped_expr);
        NFA nfa = eNFA.toRegularNFA();
        this.graph = nfa.graph;
        this.start = nfa.start;
        this.ends = nfa.ends;
    }

    public Set<RegularTransition> getNextTransitions(String vertex)
    {
        Set<DefaultEdge> outgoing = this.graph.outgoingEdgesOf(vertex);
        Set<RegularTransition> transitions = new HashSet<>();

        for (DefaultEdge edge : outgoing) 
            transitions.add(((RegularTransition)edge));

        return transitions;
    }

    public void print()
    {
        System.out.println("Start: " + this.start);
        System.out.println("Ends: " + this.ends);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
