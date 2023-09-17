package pcreToHLS;

import java.util.Set;

import org.jgrapht.*;
import org.jgrapht.graph.*;


public abstract class FinalAutomaton {
    protected String start;
    protected Set<String> ends;
    protected Graph<String, DefaultEdge> graph = new DirectedPseudograph<>(LabeledEdge.class);

    protected void copy(FinalAutomaton other)
    {
        this.graph = other.graph;
        this.start = other.start;
        this.ends = other.ends;
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
