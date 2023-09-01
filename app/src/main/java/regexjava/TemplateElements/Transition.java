package regexjava.TemplateElements;

public class Transition {
    private State target;
    private int token;
    private boolean wildcard = false;

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
}
