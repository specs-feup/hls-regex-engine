package pcreToHLS;

import java.util.Set;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
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
            
            if (this.ends.contains(vertex))
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
        System.out.println("Ends: " + this.ends);
        System.out.println("Graph:");
        System.out.println(this.graph);
    }
}
