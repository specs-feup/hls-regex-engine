package regexjava;

import org.jgrapht.graph.DefaultEdge;

class LabeledEdge extends DefaultEdge
{
    private String label;

    public LabeledEdge(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
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
}
