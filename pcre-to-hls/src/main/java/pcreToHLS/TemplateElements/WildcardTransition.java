package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.FifoInfo;

public class WildcardTransition extends Transition{
    public WildcardTransition(boolean at_start, boolean at_end, List<CounterInfo> counters_info, Set<FifoInfo> fifos_info, State source, State target)
    {
        super(TransitionType.Wildcard, at_start, at_end, counters_info, fifos_info, source, target);
    }    
}
