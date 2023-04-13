package regexjava.TemplateElements;

import java.util.HashSet;
import java.util.Set;

public class State {
    private int id;
    private Set<Transition> transitions = new HashSet<>();

    public Set<Transition> getTransitions() 
    {
        return transitions;
    }

    public void setTransitions(Set<Transition> transitions) 
    {
        this.transitions = transitions;
    }

    public int getId() 
    {
        return id;
    }

    public void setId(int id) 
    {
        this.id = id;
    }

    public void addTransition(Transition t)
    {
        this.transitions.add(t);
    }
}
