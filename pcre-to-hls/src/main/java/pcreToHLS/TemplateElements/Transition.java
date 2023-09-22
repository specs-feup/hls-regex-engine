package pcreToHLS.TemplateElements;

public class Transition {
    private State target;
    private int token;
    private boolean wildcard = false;
    private boolean negated = false;
    private boolean padding = false;

    public State getTarget() 
    {
        return target;
    }
    
    public void setTarget(State target) 
    {
        this.target = target;
    }

    public int getToken() 
    {
        return token;
    }

    public void setToken(int token) 
    {
        this.token = token;
    }

    public boolean isWildcard() 
    {
        return wildcard;
    }

    public void setWildcard(boolean wildcard) 
    {
        this.wildcard = wildcard;
    }
    
    public boolean isNegated() 
    {
        return negated;
    }

    public void setNegated(boolean negated) 
    {
        this.negated = negated;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }

}
