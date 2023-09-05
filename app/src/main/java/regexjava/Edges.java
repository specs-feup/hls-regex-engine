package regexjava;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

import regexjava.TemplateElements.State;
import regexjava.TemplateElements.Transition;

abstract class LabeledEdge<T> extends DefaultEdge
{
    protected T label;

    public LabeledEdge(T label)
    {
        this.label = label;
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
        return other_edge.label.equals(this.label) && other_edge.getSource().equals(this.getSource()) 
               && other_edge.getTarget().equals(this.getTarget());
    }

    abstract public LabeledEdge<T> copy();

    abstract public List<Transition> generateTransitions(State target);
}

class WildcardEdge extends LabeledEdge<Integer>
{
    public WildcardEdge()
    {
        super(-1);
    }

    @Override
    public String toString()
    {
        return "(" + getSource() + " -> " + getTarget() + " : " + "wildcard" + ")";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        return new WildcardEdge();
    }

    @Override
    public List<Transition> generateTransitions(State target) 
    {
        Transition transition = new Transition();
        transition.setWildcard(true);
        transition.setTarget(target);
        return Arrays.asList(transition);     
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
        return "(" + getSource() + " -> " + getTarget() + " : " + "epsilon" + ")";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        return new EpsilonEdge();
    }

    @Override
    public List<Transition> generateTransitions(State target) 
    {
        return null;
    }
}

class CharacterEdge extends LabeledEdge<Integer>
{
    public CharacterEdge(int code_point)
    {
        super(code_point);
    }

    public int getCodePoint()
    {
        return this.label;
    }

    @Override
    public String toString()
    {
        int val = this.label.intValue();
        return "(" + getSource() + " -> " + getTarget() + " : " + (char) val + ")";
    }

    @Override
    public LabeledEdge<Integer> copy() 
    {
        return new CharacterEdge(this.label);
    }

    @Override
    public List<Transition> generateTransitions(State target) 
    {
        Transition transition = new Transition();
        transition.setTarget(target);
        transition.setToken(this.label);
        return Arrays.asList(transition);      
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
        String str = "(" + getSource() + " -> " + getTarget() + " : BLOCK[";
        for (Integer e : this.label)
        {
            int val = e.intValue();
            str += (char) val;
        }

        return str + "])";
    }

    @Override
    public LabeledEdge<Integer[]> copy() 
    {
        return new CharacterBlockEdge(this.label);
    }

    @Override
    public List<Transition> generateTransitions(State target) 
    {
        return null;     
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
        String str = "(" + getSource() + " -> " + getTarget() + " : " + type + "CLASS[";
        for (Integer e : this.label)
        {
            int val = e.intValue();
            str += (char) val;
        }

        return str + "])";
    }

    @Override
    public LabeledEdge<Set<Integer>> copy() 
    {
        return new CharacterClassEdge(this.label, this.negated);
    }

    @Override
    public List<Transition> generateTransitions(State target) 
    {
        List<Transition> transitions = new LinkedList<>();
        for (int code_point : this.label)
        {
            Transition transition = new Transition();
            transition.setTarget(target);
            transition.setToken(code_point);
            transitions.add(transition);
        }
        return transitions; 
    }
}
