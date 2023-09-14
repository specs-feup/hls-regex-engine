package pcreToHLS;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RulesAnalyzer {
    private Map<String, Integer> operator_occurrences;

    private RulesAnalyzer(Map<String, Integer> operator_occurences)
    {
        this.operator_occurrences = operator_occurences;
    }

    public RulesAnalyzer()
    {
        this(new HashMap<>());
    }

    public void addOccurence(String operator)
    {
        this.operator_occurrences.merge(operator, 1, Integer::sum);
    }

    public RulesAnalyzer add(RulesAnalyzer other)
    {
        RulesAnalyzer sum = new RulesAnalyzer(this.operator_occurrences);
        for (Entry<String, Integer> entry : other.operator_occurrences.entrySet())
            sum.operator_occurrences.merge(entry.getKey(), entry.getValue(), Integer::sum);

        return sum;
    }

    public void print()
    {
        for (Entry<String, Integer> entry : this.operator_occurrences.entrySet())
            System.out.println(entry.getKey() + ":" + entry.getValue());
    }
}
