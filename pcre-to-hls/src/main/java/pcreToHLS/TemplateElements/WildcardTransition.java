package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.FifoInfo;

public class WildcardTransition extends Transition{
    private boolean padding;

    public WildcardTransition(String anchor_info, List<CounterInfo> counters_info, Set<FifoInfo> fifos_info, State source, State target, boolean padding)
    {
        super(TransitionType.Wildcard, anchor_info, counters_info, fifos_info, source, target);
        this.padding = padding;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }
    
}
