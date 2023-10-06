package pcreToHLS;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

import pcreToHLS.TemplateElements.BackreferenceTransition;
import pcreToHLS.TemplateElements.CharacterClassTransition;
import pcreToHLS.TemplateElements.CharacterTransition;
import pcreToHLS.TemplateElements.State;
import pcreToHLS.TemplateElements.Transition;
import pcreToHLS.TemplateElements.WildcardTransition;

abstract class LabeledEdge<T> extends DefaultEdge
{
    protected T label;
    protected List<CounterInfo> counter_infos;
    protected Set<FifoInfo> fifos_info;
    protected boolean at_start;
    protected boolean at_end;

    public LabeledEdge(T label, List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        this.label = label;
        this.counter_infos = new LinkedList<>();
        for (CounterInfo ci : counter_infos)
            this.counter_infos.add(new CounterInfo(ci.getCounter(), ci.getOperation()));
        this.at_start = at_start;
        this.at_end = at_end;
        this.fifos_info = new HashSet<>();
         for (FifoInfo fi : fifos_info)
            this.fifos_info.add(new FifoInfo(fi.getFifo(), fi.isClear()));
    }

    public LabeledEdge(T label, List<CounterInfo> counter_infos, Set<FifoInfo> fifos_info)
    {
        this(label, counter_infos, false, false, fifos_info);
    }

    public LabeledEdge(T label, List<CounterInfo> counter_infos)
    {
        this(label, counter_infos, new HashSet<>());
    }

    public LabeledEdge(T label, CounterInfo counter_info)
    {
        this(label, Arrays.asList(counter_info));
    }

    public LabeledEdge(T label)
    {
        this(label, new LinkedList<>());
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

    public Set<FifoInfo> getFifosInfo() {
        return fifos_info;
    }

    public void setFifosInfo(Set<FifoInfo> fifos_info)
    {
        this.fifos_info = fifos_info;
    }

    public void addFifosInfo(FifoInfo fifo_info)
    {
        this.fifos_info.add(fifo_info);
    }

    public boolean isAtStart() {
        return at_start;
    }

    public void setAtStart(boolean at_start) {
        this.at_start = at_start;
    }

    public boolean isAtEnd() {
        return at_end;
    }

    public void setAtEnd(boolean at_end) {
        this.at_end = at_end;
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
        boolean check_anchors = this.at_start == other_edge.at_start && this.at_end == other_edge.at_end;
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
        if (!this.at_start && !this.at_end)
            return "@any";

        String str = "";
        if (this.at_start)
            str += "@start";
        if (this.at_end)
            str += "@end";

        return str;
    }

    protected String getFifosString()
    {
        String str = "add_to[";
        for (FifoInfo fifo : this.fifos_info)
            str += fifo;
        return str + "]";
    }


    abstract public LabeledEdge<T> copy();
    abstract public Transition generateTransition(State source, State target);
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

    private WildcardEdge(List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(-1, counter_infos, at_start, at_end, fifos_info);
    }

    @Override
    public String toString()
    {
        String name = "wildcard";
        return name + getAnchorString() + getCountersString() + getFifosString();
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        WildcardEdge copy = new WildcardEdge(this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public WildcardTransition generateTransition(State source, State target) 
    {
        WildcardTransition transition = new WildcardTransition(this.at_start, this.at_end, this.counter_infos, this.fifos_info, source, target);
        return transition;
    }
} 

class EpsilonEdge extends LabeledEdge<Integer>
{
    public EpsilonEdge()
    {
        super(-2);
    }

    private EpsilonEdge(List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(-2, counter_infos, at_start, at_end, fifos_info);
    }

    @Override
    public String toString()
    {
        return "epsilon";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        EpsilonEdge copy = new EpsilonEdge(this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public Transition generateTransition(State source, State target) 
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

    private CharacterEdge(int code_point, List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(code_point, counter_infos, at_start, at_end, fifos_info);
    }

    public int getCodePoint()
    {
        return this.label;
    }

    @Override
    public String toString()
    {
        int val = this.label.intValue();
        return (char) val + this.getAnchorString() + this.getCountersString() + getFifosString();
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        CharacterEdge copy = new CharacterEdge(this.label, this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public CharacterTransition generateTransition(State source, State target) 
    {
        CharacterTransition transition = new CharacterTransition(this.at_start, this.at_end, this.counter_infos, this.fifos_info, source, target, label);
        return transition;
    }

}

class CharacterBlockEdge extends LabeledEdge<Integer[]>
{
    public CharacterBlockEdge(Integer[] code_points)
    {
        super(code_points);
    } 

    private CharacterBlockEdge(Integer[] code_points, List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(code_points, counter_infos, at_start, at_end, fifos_info);
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
        CharacterBlockEdge copy = new CharacterBlockEdge(this.label, this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public Transition generateTransition(State source, State target) 
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

    protected CharacterClassEdge(Set<Integer> code_points, boolean negated, List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(code_points, counter_infos, at_start, at_end, fifos_info);
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

        return str + ")" + getAnchorString() + getCountersString() + getFifosString();
    }

    @Override
    public LabeledEdge<Set<Integer>> copy() 
    {
        CharacterClassEdge copy = new CharacterClassEdge(this.label, this.negated, this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public CharacterClassTransition generateTransition(State source, State target) 
    {
        CharacterClassTransition transition = new CharacterClassTransition(this.at_start, this.at_end, this.counter_infos, this.fifos_info, source, target, label, negated);
        return transition;
    }
}

class CounterEdge extends LabeledEdge<CounterInfo>
{
    public CounterEdge(CounterInfo counter_info)
    {
        super(counter_info, counter_info);
    }

    private CounterEdge(CounterInfo counter_info, List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(counter_info, counter_infos, at_start, at_end, fifos_info);
    }

    @Override
    public String toString()
    {
        return "COUNTER " + this.label.counter.getId() + getCountersString();
    }

    @Override
    public LabeledEdge<CounterInfo> copy() 
    {
        CounterEdge copy = new CounterEdge(this.label, this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public Transition generateTransition(State source, State target) {
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
        StartAnchorEdge copy = new StartAnchorEdge();
        return copy;
    }

    @Override
    public Transition generateTransition(State source, State target) 
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
        EndAnchorEdge copy = new EndAnchorEdge();
        return copy;
    }

    @Override
    public Transition generateTransition(State source, State target) 
    {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");
    }

}

class CaptureEdge extends LabeledEdge<Fifo>
{
    public enum CaptureType {START, END};
    private CaptureType type;

    public CaptureEdge(CaptureType type, Fifo fifo)
    {
        super(fifo);
        this.type = type;
    }

    public CaptureEdge(CaptureType type)
    {
        this(type, new Fifo());
    }

    public Fifo getFifo()
    {
        return this.label;
    }

    public CaptureType getType()
    {
        return this.type;
    }

    @Override
    public String toString()
    {
        return (this.type == CaptureType.START ? "start" : "end") + " capture " + this.label;
    }

    @Override
    public LabeledEdge<Fifo> copy() 
    {
        CaptureEdge copy = new CaptureEdge(this.type, this.label);
        return copy;
    }

    @Override
    public Transition generateTransition(State source, State target) 
    {
        throw new UnsupportedOperationException("Unimplemented method 'generateTransitions'");
    }
}

class BackreferenceEdge extends LabeledEdge<Fifo>
{
    private BackreferenceEdge(int fifo_id, List<CounterInfo> counter_infos, boolean at_start, boolean at_end, Set<FifoInfo> fifos_info)
    {
        super(new Fifo(fifo_id), counter_infos, at_start, at_end, fifos_info);
    }

    public BackreferenceEdge(int fifo_id)
    {
        super(new Fifo(fifo_id));
    }


    public Fifo getFifo()
    {
        return this.label;
    }

    @Override
    public String toString()
    {
        return "backreference " + this.label + getAnchorString() + getCountersString() + getFifosString();
    }

    @Override
    public LabeledEdge<Fifo> copy() 
    {
        BackreferenceEdge copy = new BackreferenceEdge(this.label.getIdNo(), this.counter_infos, this.at_start, this.at_end, this.fifos_info);
        return copy;
    }

    @Override
    public BackreferenceTransition generateTransition(State source, State target) 
    {
        BackreferenceTransition transition = new BackreferenceTransition(this.at_start, this.at_end, this.counter_infos, this.fifos_info, source, target, label);
        return transition;
    }
}
