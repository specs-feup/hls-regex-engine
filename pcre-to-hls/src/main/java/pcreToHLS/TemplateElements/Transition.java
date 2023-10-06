package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.FifoInfo;

public abstract class Transition {
    protected enum TransitionType { Character, CharacterClass, Wildcard, Backreference};
    protected TransitionType type;
    protected State source;
    protected State target;
    protected boolean at_start;
    protected boolean at_end;
    protected Set<FifoInfo> fifos_info;
    protected List<CounterInfo> counters_info;

    protected Transition(TransitionType type, boolean at_start, boolean at_end, List<CounterInfo> counters_info, Set<FifoInfo> fifos_info, State source, State target) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.at_start = at_start;
        this.at_end = at_end;
        this.fifos_info = fifos_info;
        this.counters_info = counters_info;
    }

    public TransitionType getType() {
        return type;
    }

    public void setType(TransitionType type) {
        this.type = type;
    }

    public State getSource() {
        return source;
    }

    public void setSource(State source) {
        this.source = source;
    }

    public State getTarget() 
    {
        return target;
    }
    
    public void setTarget(State target) 
    {
        this.target = target;
    }

    public boolean isAt_start() {
        return at_start;
    }

    public void setAt_start(boolean at_start) {
        this.at_start = at_start;
    }

    public boolean isAt_end() {
        return at_end;
    }

    public void setAt_end(boolean at_end) {
        this.at_end = at_end;
    }

    public Set<FifoInfo> getFifos_info() {
        return fifos_info;
    }

    public void setFifos_info(Set<FifoInfo> fifos_info) {
        this.fifos_info = fifos_info;
    }

    public List<CounterInfo> getCounters_info() {
        return counters_info;
    }

    public void setCounters_info(List<CounterInfo> counter_info) {
        this.counters_info = counter_info;
    }
}
