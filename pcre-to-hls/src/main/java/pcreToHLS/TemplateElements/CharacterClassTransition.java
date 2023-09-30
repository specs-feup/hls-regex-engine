package pcreToHLS.TemplateElements;

import java.util.Set;

public class CharacterClassTransition extends Transition {
    private Set<Integer> tokens;
    private boolean negated;

    public CharacterClassTransition(State source, State target, Set<Integer> tokens, boolean negated)
    {
        super(TransitionType.CharacterClass, source, target);
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
