package pcreToHLS;

import java.util.Set;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class DFA {
    private String start;
    private Set<String> ends;
    private Graph<String, DefaultEdge> graph = new DirectedPseudograph<>(LabeledEdge.class);

    public DFA(Graph<String, DefaultEdge> graph, String start, Set<String> ends) 
    {
        this.graph = graph;
        this.start = start;
        this.ends = ends;
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
