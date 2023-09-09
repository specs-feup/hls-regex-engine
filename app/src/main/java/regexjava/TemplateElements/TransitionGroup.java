package regexjava.TemplateElements;

import java.util.List;

import regexjava.CounterInfo;

public class TransitionGroup {
    private List<Transition> transitions;
    private boolean intercept;
    private CounterInfo counter_info;

    public TransitionGroup(){}

    public TransitionGroup(List<Transition> transitions, boolean intercept, CounterInfo counter_info)
    {
        this.transitions = transitions;
        this.intercept = intercept;
        this.counter_info = counter_info;
    }

    public TransitionGroup(List<Transition> transitions, boolean intercept)
    {
        this(transitions, intercept, null);
    }

    public TransitionGroup(List<Transition> transitions, CounterInfo counter_info)
    {
        this(transitions, false, counter_info);
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

    public CounterInfo getCounter_info() {
        return counter_info;
    }

    public void setCounter_info(CounterInfo counter_info) {
        this.counter_info = counter_info;
    }
}
