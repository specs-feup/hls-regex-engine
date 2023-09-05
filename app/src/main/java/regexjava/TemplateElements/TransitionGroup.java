package regexjava.TemplateElements;

import java.util.List;

public class TransitionGroup {
    private List<Transition> transitions;
    private boolean intercept;

    public TransitionGroup(){}

    public TransitionGroup(List<Transition> transitions, boolean intercept)
    {
        this.transitions = transitions;
        this.intercept = intercept;
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
}
