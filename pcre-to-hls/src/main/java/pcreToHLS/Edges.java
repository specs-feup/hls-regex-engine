package pcreToHLS;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

import pcreToHLS.TemplateElements.State;
import pcreToHLS.TemplateElements.Transition;
import pcreToHLS.TemplateElements.TransitionGroup;

abstract class LabeledEdge<T> extends DefaultEdge
{
    public enum AnchorType { NONE, START, END };

    protected T label;
    protected List<CounterInfo> counter_infos;
    protected AnchorType anchor_info = AnchorType.NONE;

    public LabeledEdge(T label)
    {
        this.label = label;
        this.counter_infos = new LinkedList<>();
    }

    public LabeledEdge(T label, CounterInfo counter_info)
    {
        this.label = label;
        this.counter_infos = new LinkedList<>(Arrays.asList(counter_info));
    }

    public LabeledEdge(T label, List<CounterInfo> counter_infos)
    {
        this.label = label;
        this.counter_infos = new LinkedList<>(counter_infos);
    }

    public List<CounterInfo> getCounterInfos() {
        return counter_infos;
    }

    public void setCounterInfos(List<CounterInfo> counter_infos)
    {
        this.counter_infos = counter_infos;
    }

    public void addCounterInfos(List<CounterInfo> counter_infos) 
    {
        this.counter_infos.addAll(counter_infos);
    }

    public AnchorType getAnchorInfo() 
    {
        return anchor_info;
    }

    public void setAnchorInfo(AnchorType anchor_info) 
    {
        this.anchor_info = anchor_info;
    }

    @Override
    public int hashCode() 
    {
        return Objects.hashCode(this.label);
    }

    @Override
    public boolean equals(Object other) 
    {
        if (this == other)
            return true;

        if (other == null || getClass() != other.getClass())
            return false;

        LabeledEdge<?> other_edge = (LabeledEdge<?>) other;
        boolean check_anchors = this.anchor_info.equals(other_edge.anchor_info);
        boolean check_counter_info = this.counter_infos.equals(other_edge.counter_infos);
        boolean check_source = this.getSource() != null ? this.getSource().equals(other_edge.getSource()) : other_edge.getSource() == null;
        boolean check_target = this.getTarget() != null ? this.getTarget().equals(other_edge.getTarget()) : other_edge.getTarget() == null;
        return other_edge.label.equals(this.label) && check_anchors && check_counter_info && check_source && check_target;
    }

    protected String getCountersString()
    {
        String str = "[";
        for (int i = 0; i < this.counter_infos.size(); i++)
        {
            if (i != 0)
                str += " & ";
            str += this.counter_infos.get(i);
        }
        return str + "]";
    }

    protected String getAnchorString()
    {
        String str;
        switch (this.anchor_info)
        {
            case END:
                str = "@end";
                break;
            case START:
                str = "@start";
                break;
            default:
                str = "@any";
                break;
        }

        return str;
    }

    abstract public LabeledEdge<T> copy();

    abstract public TransitionGroup generateTransitions(State target);
}

class WildcardEdge extends LabeledEdge<Integer>
{
    public WildcardEdge()
    {
        super(-1);
    }

    public WildcardEdge(CounterInfo counter_info)
    {
        super(-1, counter_info);
    }

    public WildcardEdge(List<CounterInfo> counter_infos)
    {
        super(-1, counter_infos);
    }

    @Override
    public String toString()
    {
        return "wildcard" + getAnchorString() + getCountersString();
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        LabeledEdge<Integer> copy = new WildcardEdge(this.counter_infos);
        copy.setAnchorInfo(this.anchor_info);
        return copy;
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        Transition transition = new Transition();
        transition.setWildcard(true);
        transition.setTarget(target);
        TransitionGroup group = new TransitionGroup(Arrays.asList(transition), this.counter_infos);
        group.setAnchor_info(this.anchor_info.name());
        return group;
    }
} 

class EpsilonEdge extends LabeledEdge<Integer>
{
    public EpsilonEdge()
    {
        super(-2);
    }

    @Override
    public String toString()
    {
        return "epsilon";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        LabeledEdge<Integer> copy = new EpsilonEdge();
        copy.setAnchorInfo(this.anchor_info);
        return copy;
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");
    }
}

class CharacterEdge extends LabeledEdge<Integer>
{
    public CharacterEdge(int code_point)
    {
        super(code_point);
    }

    public CharacterEdge(int code_point, CounterInfo counter_info)
    {
        super(code_point, counter_info);
    }

    public CharacterEdge(int code_point, List<CounterInfo> counter_infos)
    {
        super(code_point, counter_infos);
    }

    public int getCodePoint()
    {
        return this.label;
    }

    @Override
    public String toString()
    {
        int val = this.label.intValue();
        return (char) val + this.getAnchorString() + this.getCountersString();
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        LabeledEdge<Integer> copy =  new CharacterEdge(this.label, this.counter_infos);
        copy.setAnchorInfo(this.anchor_info);
        return copy;
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        Transition transition = new Transition();
        transition.setTarget(target);
        transition.setToken(this.label);
        TransitionGroup group = new TransitionGroup(Arrays.asList(transition), this.counter_infos);
        group.setAnchor_info(this.anchor_info.name());
        return group;
    }
}

class CharacterBlockEdge extends LabeledEdge<Integer[]>
{
    public CharacterBlockEdge(Integer[] code_points)
    {
        super(code_points);
    } 

    public Integer[] getCodePoints()
    {
        return this.label;
    }

    @Override
    public String toString()
    {
        String str = "BLOCK(";
        for (Integer e : this.label)
        {
            int val = e.intValue();
            str += (char) val;
        }

        return str + ")";
    }

    @Override
    public LabeledEdge<Integer[]> copy() 
    {
        LabeledEdge<Integer[]> copy = new CharacterBlockEdge(this.label);
        copy.setAnchorInfo(this.anchor_info);
        return copy;
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");  
    }
}

class CharacterClassEdge extends LabeledEdge<Set<Integer>>
{
    private boolean negated;

    public CharacterClassEdge(Set<Integer> code_points, boolean negated)
    {
        super(code_points);
        this.negated = negated;
    }

    public CharacterClassEdge(Set<Integer> code_points, boolean negated, CounterInfo counter_info)
    {
        super(code_points, counter_info);
        this.negated = negated;
    }

    public CharacterClassEdge(Set<Integer> code_points, boolean negated, List<CounterInfo> counter_infos)
    {
        super(code_points, counter_infos);
        this.negated = negated;
    }

    public Set<Integer> getCodePoints()
    {
        return this.label;
    }

    public boolean isNegated()
    {
        return this.negated;
    }

    @Override
    public String toString()
    {
        String type = this.negated ? "NEGATED_" : "";
        String str = type + "CLASS(";
        for (Integer e : this.label)
        {
            int val = e.intValue();
            str += (char) val;
        }

        return str + ")" + getCountersString();
    }

    @Override
    public LabeledEdge<Set<Integer>> copy() 
    {
        LabeledEdge<Set<Integer>> copy = new CharacterClassEdge(this.label, this.negated, this.counter_infos);
        copy.setAnchorInfo(this.anchor_info);
        return copy;
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        List<Transition> transitions = new LinkedList<>();
        for (int code_point : this.label)
        {
            Transition transition = new Transition();
            transition.setTarget(target);
            transition.setToken(code_point);
            transition.setNegated(this.negated);
            transitions.add(transition);
        }
        TransitionGroup group = new TransitionGroup(transitions, this.negated, this.counter_infos);
        group.setAnchor_info(this.anchor_info.name());
        return group;
    }
}

class CounterEdge extends LabeledEdge<CounterInfo>
{
    public CounterEdge(CounterInfo counter_info)
    {
        super(counter_info, counter_info);
    }

    @Override
    public String toString()
    {
        return "COUNTER " + this.label.counter.getId() + getCountersString();
    }

    @Override
    public LabeledEdge<CounterInfo> copy() 
    {
        LabeledEdge<CounterInfo> copy = new CounterEdge(this.counter_infos.get(0));
        copy.setAnchorInfo(this.anchor_info);
        return copy;
    }

    @Override
    public TransitionGroup generateTransitions(State target) {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");
    }
}

class StartAnchorEdge extends LabeledEdge<Integer>
{
    public StartAnchorEdge()
    {
        super(-3);
    }

    @Override
    public String toString()
    {
        return "start anchor";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        return new StartAnchorEdge();
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");
    }
}


class EndAnchorEdge extends LabeledEdge<Integer>
{
    public EndAnchorEdge()
    {
        super(-4);
    }

    @Override
    public String toString()
    {
        return "end anchor";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        return new EndAnchorEdge();
    }

    @Override
    public TransitionGroup generateTransitions(State target) 
    {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");
    }
}
