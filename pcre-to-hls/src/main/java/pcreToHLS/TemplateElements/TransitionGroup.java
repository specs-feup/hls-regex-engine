package pcreToHLS.TemplateElements;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import pcreToHLS.CounterInfo;
import pcreToHLS.Fifo;

public class TransitionGroup {
    private List<Transition> transitions;
    private boolean intercept;
    private List<CounterInfo> counter_infos;
    private String anchor_info;
    private Set<Fifo> fifos_info;

    public TransitionGroup(){}

    public TransitionGroup(List<Transition> transitions, boolean intercept, List<CounterInfo> counter_infos)
    {
        this.transitions = transitions;
        this.intercept = intercept;
        this.counter_infos = counter_infos;
    }

    public TransitionGroup(List<Transition> transitions, boolean intercept)
    {
        this(transitions, intercept, new LinkedList<>());
    }

    public TransitionGroup(List<Transition> transitions, List<CounterInfo> counter_infos)
    {
        this(transitions, false, counter_infos);
    }

    public TransitionGroup(List<Transition> transitions)
    {
        this(transitions, false);
    }

    public boolean isIntercept() {
        return intercept;
    }

    public void setIntercept(boolean intercept) {
        this.intercept = intercept;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> group) {
        this.transitions = group;
    }

    public void addTransition(Transition t)
    {
        this.transitions.add(t);
    }

    public List<CounterInfo> getCounter_infos() {
        return counter_infos;
    }

    public void setCounter_infos(List<CounterInfo> counter_infos) {
        this.counter_infos = counter_infos;
    }

    public String getAnchor_info() {
        return anchor_info;
    }

    public void setAnchor_info(String anchor_info) {
        this.anchor_info = anchor_info;
    }

    public Set<Fifo> getFifos_info() {
        return fifos_info;
    }

    public void setFifos_info(Set<Fifo> fifos_info) {
        this.fifos_info = fifos_info;
    }
}
