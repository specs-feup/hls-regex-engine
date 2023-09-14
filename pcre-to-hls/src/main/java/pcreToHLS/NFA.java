package pcreToHLS;

import java.util.EmptyStackException;
import java.util.Set;

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
        // System.out.println("\n=== NFA ===");
        // nfa.print();
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


    public void print()
    {
        System.out.println("Start: " + this.start);
        System.out.println("Ends: " + this.ends);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
