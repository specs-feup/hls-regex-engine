package pcreToHLS.TemplateElements;

public class CharacterTransition extends Transition {
    private int token;

    public CharacterTransition(String anchor_info, State source, State target, int token)
    {
        super(TransitionType.Character, anchor_info, source, target);
        this.token = token;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }
    
}
