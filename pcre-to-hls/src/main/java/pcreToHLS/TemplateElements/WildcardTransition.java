package pcreToHLS.TemplateElements;

public class WildcardTransition extends Transition{
    private boolean padding;

    public WildcardTransition(String anchor_info, State source, State target, boolean padding)
    {
        super(TransitionType.Wildcard, anchor_info, source, target);
        this.padding = padding;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }
    
}
