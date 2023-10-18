package pcreToHLS;

import java.util.EmptyStackException;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.jgrapht.*;
import org.jgrapht.graph.*;


public class DFA extends FinalAutomaton {

    public DFA(Graph<String, DefaultEdge> graph, String start, Set<String> ends) 
    {
        this.graph = graph;
        this.start = start;
        this.ends = ends;
    }

    public DFA(ParseTree root, String flags, boolean debug, boolean remove_unused, boolean expand_fixed) throws EmptyStackException 
    {
        EpsilonNFA eNFA = new EpsilonNFA(root, flags);
        NFA nfa = eNFA.toRegularNFA(flags.contains("m"), remove_unused, expand_fixed);
        DFA dfa = nfa.toDFA();
        if (debug)
        {
            System.out.println("\n=== DFA ===");
            dfa.print();
            dfa.display();
        }
        copy(dfa);
    }
}
