package regexjava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Matcher {
    private NFA automata;

    public Matcher(NFA automata)
    {
        this.automata = automata;
    }

    public Matcher(String raw_regex)
    {
        this.automata = new NFA(raw_regex);
    }

    private String getNext(Set<RegularTransition> transitions, char token)
    {
        for (RegularTransition transition : transitions) 
        {
            if (transition.getSymbol() == token)
                return this.automata.graph.getEdgeTarget(transition);
        }

        return null;
    }

    public boolean match(String str)
    {
        Set<String> current = new HashSet<>(Arrays.asList(automata.start));
        
        for (char token : str.toCharArray()) 
        {
            Set<String> next = new HashSet<>();
            for (String state : current)
            {
                Set<RegularTransition> transitions = this.automata.getNextTransitions(state);
                String next_node = getNext(transitions, token);
                if (next_node != null)
                    next.add(next_node);
            }

            current = Set.copyOf(next);
        }

        Set<String> intersection = new HashSet<String>(current);
        intersection.retainAll(this.automata.ends);
        return !intersection.isEmpty();
    }
}
