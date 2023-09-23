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

    public DFA(ParseTree root, RulesAnalyzer analyzer, String flags) throws EmptyStackException 
    {
        EpsilonNFA eNFA = new EpsilonNFA(root, analyzer, flags);
        NFA nfa = eNFA.toRegularNFA();
        DFA dfa = nfa.toDFA();
        System.out.println("\n=== DFA ===");
        dfa.print();
        dfa.display();
        copy(dfa);
    }
}
