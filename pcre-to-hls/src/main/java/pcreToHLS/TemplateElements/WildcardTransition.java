package pcreToHLS.TemplateElements;

public class WildcardTransition extends Transition{
    private boolean padding;

    public WildcardTransition(State source, State target, boolean padding)
    {
        super(TransitionType.Wildcard, source, target);
        this.padding = padding;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }
    
}
