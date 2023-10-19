package pcreToHLS.TemplateElements;
import java.util.List;
import java.util.LinkedList;

public class State {
    private int id;
    private List<Transition> outgoing_transitions = new LinkedList<>();

    public void addTransition(Transition t)
    {
        this.outgoing_transitions.add(t);
    }

    public int getId() 
    {
        return id;
    }

    public void setId(int id) 
    {
        this.id = id;
    }

    public List<Transition> getOutgoing_transitions() 
    {
        return outgoing_transitions;
    }

    public void setOutgoing_transitions(List<Transition> outgoing_transitions) 
    {
        this.outgoing_transitions = outgoing_transitions;
    }
}
