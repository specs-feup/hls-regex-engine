package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.FifoInfo;

public class CharacterClassTransition extends Transition {
    private Set<Integer> tokens;
    private boolean negated;

    public CharacterClassTransition(boolean at_start, boolean at_end, List<CounterInfo> counters_info, Set<FifoInfo> fifos_info, State source, State target, Set<Integer> tokens, boolean negated)
    {
        super(TransitionType.CharacterClass, at_start, at_end, counters_info, fifos_info, source, target);
        this.tokens = tokens;
        this.negated = negated;
    }

    public Set<Integer> getTokens() {
        return tokens;
    }

    public void setTokens(Set<Integer> tokens) {
        this.tokens = tokens;
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }
    
}
