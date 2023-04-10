package regexjava;

import java.util.Stack;

import org.jgrapht.*;
import org.jgrapht.graph.*;

public class NFA {

    private Graph<String, DefaultEdge> graph = new DirectedMultigraph<>(LabeledEdge.class);
    private String start;
    private String end;

    public NFA(Graph<String, DefaultEdge> graph, String start, String end) 
    {
        this.graph = graph;
        this.start = start;
        this.end = end;
    }

    /* Creates Empty Transition NFA */
    public NFA() 
    {
        String v1 = VertexIDFactory.getNewVertexID();
        String v2 = VertexIDFactory.getNewVertexID();
        this.graph.addVertex(v1);
        this.graph.addVertex(v2);
        this.graph.addEdge(v1, v2, new EpsilonTransition());
        this.start = v1;
        this.end = v2;
    }

    /* Creates Single Transition NFA */
    public NFA(char symbol) 
    {
        String v1 = VertexIDFactory.getNewVertexID();
        String v2 = VertexIDFactory.getNewVertexID();
        this.graph.addVertex(v1);
        this.graph.addVertex(v2);
        this.graph.addEdge(v1, v2, new RegularTransition(symbol));
        this.start = v1;
        this.end = v2;
    }

    /* Creates NFA from a postfix expression [ such as 'ab|' for '(a|b)' ] */
    public NFA(String postfix_expr)
    {
        Stack<NFA> stack = new Stack<>();

        for (final char token : postfix_expr.toCharArray()) 
        {
            if (token == '.') 
            {
                NFA second = stack.pop();
                NFA first = stack.pop();
                stack.push(concat(first, second));
            } 
            else if (token == '|') 
            {
                NFA second = stack.pop();
                NFA first = stack.pop();
                stack.push(join(first, second));
            } 
            else if(token == '*')
            {
                NFA top = stack.pop();
                stack.push(closure(top));
            }
            else if (token == '+')
            {
                NFA top = stack.pop();
                stack.push(oneOrMore(top));
            }
            else if (token == '?')
            {
                NFA top = stack.pop();
                stack.push(zeroOrOne(top));
            }
            else 
                stack.push(new NFA(token));
        }

        NFA top = stack.peek();
        this.start = top.start;
        this.end = top.end;
        this.graph = top.graph;
        System.out.println("START: " + this.start);
        System.out.println("END: " + this.end);
        System.out.println(this.graph);
    }

    public static NFA concat(NFA first, NFA second) {
        NFA res = null;
        if (Graphs.addGraph(first.graph, second.graph)) {
            res = new NFA(first.graph, first.start, second.end);
            res.graph.addEdge(first.end, second.start, new EpsilonTransition());
        }

        return res;
    }

    public static NFA join(NFA first, NFA second)
    {
        NFA res = null;
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        first.graph.addVertex(new_start);
        second.graph.addVertex(new_start);
        first.graph.addEdge(new_start, first.start, new EpsilonTransition());
        second.graph.addEdge(new_start, second.start, new EpsilonTransition());

        first.graph.addVertex(new_end);
        second.graph.addVertex(new_end);
        first.graph.addEdge(first.end, new_end, new EpsilonTransition());
        second.graph.addEdge(second.end, new_end, new EpsilonTransition());

        if (Graphs.addGraph(first.graph, second.graph))
            res = new NFA(first.graph, new_start, new_end);

        return res;
    }

    public static NFA closure(NFA automata)
    {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(new_start, new_end, new EpsilonTransition());

        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonTransition());

        return new NFA(automata.graph, new_start, new_end);
    }

    NFA oneOrMore(NFA automata)
    {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());
        automata.graph.addEdge(automata.end, automata.start, new EpsilonTransition());

        return new NFA(automata.graph, new_start, new_end);
    }

    NFA zeroOrOne(NFA automata)
    {
        String new_start = VertexIDFactory.getNewVertexID();
        String new_end = VertexIDFactory.getNewVertexID();
        automata.graph.addVertex(new_start);
        automata.graph.addVertex(new_end);

        automata.graph.addEdge(new_start, new_end, new EpsilonTransition());
        automata.graph.addEdge(new_start, automata.start, new EpsilonTransition());
        automata.graph.addEdge(automata.end, new_end, new EpsilonTransition());

        return new NFA(automata.graph, new_start, new_end);
    }
}
