package regexjava.TemplateElements;

import java.util.LinkedList;
import java.util.List;

public class State {
    private int id;
    private List<TransitionGroup> transition_groups = new LinkedList<>();

    public List<TransitionGroup> getTransition_groups() 
    {
        return transition_groups;
    }

    public void setTransition_groups(List<TransitionGroup> transitions) 
    {
        this.transition_groups = transitions;
    }

    public int getId() 
    {
        return id;
    }

    public void setId(int id) 
    {
        this.id = id;
    }

    public void addTransitionGroup(TransitionGroup tg)
    {
        this.transition_groups.add(tg);
    }
}
