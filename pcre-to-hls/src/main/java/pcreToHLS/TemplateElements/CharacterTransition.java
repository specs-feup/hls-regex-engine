package pcreToHLS.TemplateElements;

import java.util.Set;

import pcreToHLS.FifoInfo;

public class CharacterTransition extends Transition {
    private int token;

    public CharacterTransition(String anchor_info, Set<FifoInfo> fifos_info, State source, State target, int token)
    {
        super(TransitionType.Character, anchor_info, fifos_info, source, target);
        this.token = token;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }
    
}
