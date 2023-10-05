package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.Fifo;
import pcreToHLS.FifoInfo;

public class BackreferenceTransition extends Transition {
    private Fifo fifo_to_match;

    public BackreferenceTransition(String anchor_info, List<CounterInfo> counters_info, Set<FifoInfo> fifos_info, State source, State target, Fifo fifo_to_match)
    {
        super(TransitionType.Backreference, anchor_info, counters_info, fifos_info, source, target);
        this.fifo_to_match = fifo_to_match;
    }

    public Fifo getFifo_to_match() {
        return fifo_to_match;
    }

    public void setFifo_to_match(Fifo fifo_to_match) {
        this.fifo_to_match = fifo_to_match;
    }

    
}