package regexjava;

import org.jgrapht.graph.DefaultEdge;

class LabeledEdge extends DefaultEdge
{
    protected int label;

    public LabeledEdge(int label)
    {
        this.label = label;
    }

    public int getLabel()
    {
        return label;
    }

    @Override
    public int hashCode() 
    {
        int hash = 7;
        int sum = this.label;
        // int sum = 0;
        // for (char c : this.label.toCharArray()) 
        //     sum += c;

        hash = 71 * hash + sum;
        return hash;
    }


    @Override
    public boolean equals(Object other) 
    {
        if (this == other)
            return true;

        if (!(other instanceof LabeledEdge))
            return false;

        LabeledEdge otherEdge = (LabeledEdge) other;
        return otherEdge.getLabel() == this.label && otherEdge.getSource().equals(this.getSource()) 
               && otherEdge.getTarget().equals(this.getTarget());
    }

    @Override
    public String toString()
    {
        return "(" + getSource() + " -> " + getTarget() + " : " + (char)label + ")";
    }
}

class WildcardTransition extends LabeledEdge
{
    public WildcardTransition()
    {
        super(-1);
    }
} 

class EpsilonTransition extends LabeledEdge
{
    public EpsilonTransition()
    {
        super(-2);
    }
}

class RegularTransition extends LabeledEdge
{
    public RegularTransition(int code_point)
    {
        super(code_point);
    }

    public int getCodePoint()
    {
        return this.label;
    }
}
