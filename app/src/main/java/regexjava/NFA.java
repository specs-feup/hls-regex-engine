package regexjava;

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

    public void print()
    {
        System.out.println("Start: " + this.start);
        System.out.println("Ends: " + this.ends);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
