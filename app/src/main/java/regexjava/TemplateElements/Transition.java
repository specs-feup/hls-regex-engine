package regexjava.TemplateElements;

public class Transition {
    private State target;
    private char token;

    public State getTarget() 
    {
        return target;
    }
    
    public void setTarget(State target) 
    {
        this.target = target;
    }

    public char getToken() 
    {
        return token;
    }

    public void setToken(char token) 
    {
        this.token = token;
    }

    
}
