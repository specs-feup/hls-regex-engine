package pcreAnalyzer;

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
    private List<Double> expression_lengths;
    private List<Double> capture_group_lengths;

    public RulesAnalyzer()
    {
        this.total_operator_occurrences = new HashMap<>();
        this.expression_operator_occurrences = new HashMap<>();
        this.flag_occurrences = new HashMap<>();
        this.expression_lengths = new ArrayList<>();
        this.capture_group_lengths = new ArrayList<>();
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

    public void addExpressionLength(double expression_length)
    {
        this.expression_lengths.add(expression_length);
    }

    public void addCaptureGroupLengths(List<Double> group_lengths)
    {
        this.capture_group_lengths.addAll(group_lengths);
    }

    public void add(RulesAnalyzer other)
    {
        this.expression_lengths.addAll(other.expression_lengths);
        this.capture_group_lengths.addAll(other.capture_group_lengths);
        other.total_operator_occurrences.forEach((key, value) -> this.total_operator_occurrences.merge(key, value, Integer::sum));
        other.expression_operator_occurrences.forEach((key, value) -> this.expression_operator_occurrences.merge(key, value, Integer::sum));
        other.flag_occurrences.forEach((key, value) -> this.flag_occurrences.merge(key, value, Integer::sum));
    }

    private static double getAverage(List<Double> data)
    {
        float sum = 0;
        for(Double value : data)
            sum += value;
        return sum / data.size();
    }

    private static double getMedian(List<Double> data)
    {
        Collections.sort(data);
        if (data.size() % 2 == 0)
        {
            double middle1 = data.get(data.size() / 2 - 1);
            double middle2 = data.get(data.size() / 2);
            return (middle1 + middle2) / 2.0;
        }
        else 
            return data.get(data.size() / 2);
    }

    private static int removeInfinities(List<Double> data)
    {
        int count = 0;
        for (int i = 0; i < data.size(); i++)
        {
            if (Double.isInfinite(data.get(i)))
            {
                data.remove(i);
                count++;
            }
        }        
        return count;
    }

    public void print()
    {
        System.out.println("Expressions with Unknown Length: " + removeInfinities(this.expression_lengths));
        if (!expression_lengths.isEmpty())
        {
            System.out.println("Average Expression Length: " + getAverage(this.expression_lengths));
            System.out.println("Median Expression Length: " + getMedian(this.expression_lengths));
        }
        System.out.println("");
        System.out.println("Capture Groups with Unknown Length: " + removeInfinities(this.capture_group_lengths));
        if (!capture_group_lengths.isEmpty())
        {
            System.out.println("Average Capture Group Length: " + getAverage(this.capture_group_lengths));
            System.out.println("Median Capture Group Length: " + getMedian(this.capture_group_lengths));
        }

        System.out.println("\nPCRE Flags: ");
        for (Entry<Character, Integer> entry : this.flag_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": " + entry.getValue());

        System.out.println("\nTotal PCRE Operators: ");
        for (Entry<String, Integer> entry : this.total_operator_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": " + entry.getValue());

        System.out.println("\nExpression PCRE Operators: ");
        for (Entry<String, Integer> entry : this.expression_operator_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": in " + entry.getValue() + " expressions");
    }
}
