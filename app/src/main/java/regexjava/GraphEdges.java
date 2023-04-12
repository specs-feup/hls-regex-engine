package regexjava;

import org.jgrapht.graph.DefaultEdge;

class LabeledEdge extends DefaultEdge
{
    protected String label;

    public LabeledEdge(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public int hashCode() 
    {
        int hash = 7;
        int sum = 0;
        for (char c : this.label.toCharArray()) 
            sum += c;

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
        return otherEdge.label.equals(label) && otherEdge.getSource().equals(this.getSource()) 
               && otherEdge.getTarget().equals(this.getTarget());
    }

    @Override
    public String toString()
    {
        return "(" + getSource() + " : " + getTarget() + " : " + label + ")";
    }
}

class EpsilonTransition extends LabeledEdge
{
    public EpsilonTransition()
    {
        super("epsilon");
    }
}

class RegularTransition extends LabeledEdge
{
    public RegularTransition(char character)
    {
        super(String.valueOf(character));
    }

    public char getSymbol()
    {
        return this.label.charAt(0);
    }
}
