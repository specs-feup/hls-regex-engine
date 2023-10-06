package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.FifoInfo;

public class CharacterTransition extends Transition {
    private int token;

    public CharacterTransition(boolean at_start, boolean at_end, List<CounterInfo> counters_info, Set<FifoInfo> fifos_info, State source, State target, int token)
    {
        super(TransitionType.Character, at_start, at_end, counters_info, fifos_info, source, target);
        this.token = token;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }
    
}
