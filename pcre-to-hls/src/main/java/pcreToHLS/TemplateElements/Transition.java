package pcreToHLS.TemplateElements;

public abstract class Transition {
    protected enum TransitionType { Character, CharacterClass, Wildcard};
    protected TransitionType type;
    protected State source;
    protected State target;
    protected String anchor_info;

    protected Transition(TransitionType type, String anchor_info, State source, State target) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.anchor_info = anchor_info;
    }

    public TransitionType getType() {
        return type;
    }

    public void setType(TransitionType type) {
        this.type = type;
    }

    public State getSource() {
        return source;
    }

    public void setSource(State source) {
        this.source = source;
    }

    public State getTarget() 
    {
        return target;
    }
    
    public void setTarget(State target) 
    {
        this.target = target;
    }

    public String getAnchor_info() {
        return anchor_info;
    }

    public void setAnchor_info(String anchor_info) {
        this.anchor_info = anchor_info;
    }
}
