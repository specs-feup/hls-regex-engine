package pcreToHLS.TemplateElements;

public class CharacterTransition extends Transition {
    private int token;

    public CharacterTransition(State source, State target, int token)
    {
        super(TransitionType.Character, source, target);
        this.token = token;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }
    
}
