package pcreToHLS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RulesAnalyzer {
    private Map<String, Integer> total_operator_occurrences;
    private Map<String, Integer> expression_operator_occurrences;
    private Map<Character, Integer> flag_occurrences;
    private List<Integer> expression_lengths;

    public RulesAnalyzer()
    {
        this.total_operator_occurrences = new HashMap<>();
        this.expression_operator_occurrences = new HashMap<>();
        this.flag_occurrences = new HashMap<>();
        this.expression_lengths = new ArrayList<>();
    }

    public void addOperatorOccurrence(String operator)
    {
        this.total_operator_occurrences.merge(operator, 1, Integer::sum);
        this.expression_operator_occurrences.put(operator, 1);
    }

    private void addFlagOccurrence(char flag)
    {
        this.flag_occurrences.merge(flag, 1, Integer::sum);
    }

    public void addFlagsOccurrence(String flags)
    {
        for (char flag : flags.toCharArray())
            addFlagOccurrence(flag);
    }

    public void addExpressionLength(int expression_length)
    {
        this.expression_lengths.add(expression_length);
    }

    public void add(RulesAnalyzer other)
    {
        this.expression_lengths.addAll(other.expression_lengths);
        other.total_operator_occurrences.forEach((key, value) -> this.total_operator_occurrences.merge(key, value, Integer::sum));
        other.expression_operator_occurrences.forEach((key, value) -> this.expression_operator_occurrences.merge(key, value, Integer::sum));
        other.flag_occurrences.forEach((key, value) -> this.flag_occurrences.merge(key, value, Integer::sum));
    }

    private static float getAverage(List<Integer> data)
    {
        float sum = 0;
        for(Integer value : data)
            sum += value;

        return sum / data.size();
    }

    private static float getMedian(List<Integer> data)
    {
        Collections.sort(data);
        if (data.size() % 2 == 0)
        {
            int middle1 = data.get(data.size() / 2 - 1);
            int middle2 = data.get(data.size() / 2);
            return (float) (middle1 + middle2) / 2;
        }
        else 
            return data.get(data.size() / 2);
    }

    private float getAverageExpressionLength()
    {
        return getAverage(this.expression_lengths);
    }
    
    private float getMedianExpressionLength()
    {
        return getMedian(this.expression_lengths);
    }

    public void print()
    {
        System.out.println("Average Expression Length: " + this.getAverageExpressionLength());
        System.out.println("Median Expression Length: " + this.getMedianExpressionLength());

        System.out.println("PCRE Flags: ");
        for (Entry<Character, Integer> entry : this.flag_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": " + entry.getValue());

        System.out.println("Total PCRE Operators: ");
        for (Entry<String, Integer> entry : this.total_operator_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": " + entry.getValue());

        System.out.println("Expression PCRE Operators: ");
        for (Entry<String, Integer> entry : this.expression_operator_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": in " + entry.getValue() + " expressions");
    }
}
