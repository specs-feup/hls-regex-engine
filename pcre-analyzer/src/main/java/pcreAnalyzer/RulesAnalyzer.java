package pcreAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;


public class RulesAnalyzer {
    private String name;
    private int expression_no;
    private Map<String, Integer> total_operator_occurrences;
    private Map<String, Integer> expression_operator_occurrences;
    private Map<Character, Integer> flag_occurrences;
    private Map<Integer, Integer> bounded_quantifier_occurrences;
    private List<Double> expression_lengths;
    private List<Double> capture_group_lengths;
    private List<Double> referenced_capture_group_lengths;
    private List<Double> fixed_referenced_capture_group_lengths;

    public RulesAnalyzer(String name)
    {
        this.name = name;
        this.expression_no = 0;
        this.total_operator_occurrences = new HashMap<>();
        this.expression_operator_occurrences = new HashMap<>();
        this.flag_occurrences = new HashMap<>();
        this.bounded_quantifier_occurrences = new TreeMap<>();
        this.expression_lengths = new ArrayList<>();
        this.capture_group_lengths = new ArrayList<>();
        this.referenced_capture_group_lengths = new ArrayList<>();
        this.fixed_referenced_capture_group_lengths = new ArrayList<>();
    }

    public RulesAnalyzer()
    {
        this("");
        this.expression_no = 1;
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

    public void addQuantifierOccurence(int repetitions)
    {
        this.bounded_quantifier_occurrences.merge(repetitions, 1, Integer::sum);
    }

    public void addExpressionLength(double expression_length)
    {
        this.expression_lengths.add(expression_length);
    }

    public void addCaptureGroupLengths(List<Double> group_lengths)
    {
        this.capture_group_lengths.addAll(group_lengths);
    }

    public void addReferencedCaptureGroupLengths(List<Double> referenced_group_lengths)
    {
        this.referenced_capture_group_lengths.addAll(referenced_group_lengths);
    }

    public void addFixedReferencedCaptureGroupLengths(List<Double> fixed_referenced_group_lengths)
    {
        this.fixed_referenced_capture_group_lengths.addAll(fixed_referenced_group_lengths);
    }

    public void add(RulesAnalyzer other)
    {
        this.expression_no += other.expression_no;
        this.expression_lengths.addAll(other.expression_lengths);
        this.capture_group_lengths.addAll(other.capture_group_lengths);
        this.referenced_capture_group_lengths.addAll(other.referenced_capture_group_lengths);
        this.fixed_referenced_capture_group_lengths.addAll(other.fixed_referenced_capture_group_lengths);
        other.total_operator_occurrences.forEach((key, value) -> this.total_operator_occurrences.merge(key, value, Integer::sum));
        other.expression_operator_occurrences.forEach((key, value) -> this.expression_operator_occurrences.merge(key, value, Integer::sum));
        other.flag_occurrences.forEach((key, value) -> this.flag_occurrences.merge(key, value, Integer::sum));
        other.bounded_quantifier_occurrences.forEach((key, value) -> this.bounded_quantifier_occurrences.merge(key, value, Integer::sum));
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
                i--;
            }
        }        
        return count;
    }

    public Map<String, String> getData()
    {
        Map<String, String> data_pairs = new LinkedHashMap<>();
        List<Double> expression_length_copy = new LinkedList<>(this.expression_lengths);
        List<Double> capture_groups_length_copy = new LinkedList<>(this.capture_group_lengths);
        List<Double> referenced_capture_groups_length_copy = new LinkedList<>(this.referenced_capture_group_lengths);

        data_pairs.put("Ruleset", this.name);
        data_pairs.put("Number of Expressions", this.getFormattedValue((double) this.expression_no));

        data_pairs.put("Expressions with unknown length", this.getFormattedValue((double) removeInfinities(expression_length_copy)));
        if (!expression_length_copy.isEmpty())
        {
            data_pairs.put("Average expression length", this.getFormattedValue(getAverage(expression_length_copy)));
            data_pairs.put("Median expression length", this.getFormattedValue(getMedian(expression_length_copy)));
        }

        data_pairs.put("Capture Groups", this.getFormattedValue((double) this.capture_group_lengths.size()));
        data_pairs.put("Capture Groups with unknown length", this.getFormattedValue((double) removeInfinities(capture_groups_length_copy)));
        if (!capture_groups_length_copy.isEmpty())
        {
            data_pairs.put("Average capture group length", this.getFormattedValue(getAverage(capture_groups_length_copy)));
            data_pairs.put("Median capture group length", this.getFormattedValue(getMedian(capture_groups_length_copy)));
        }

        data_pairs.put("Referenced Capture Groups", this.getFormattedValue((double) this.referenced_capture_group_lengths.size()));
        data_pairs.put("Referenced Capture Groups with unknown length", this.getFormattedValue((double) removeInfinities(referenced_capture_groups_length_copy)));
        if (!referenced_capture_groups_length_copy.isEmpty())
        {
            data_pairs.put("Average referenced capture group length", this.getFormattedValue(getAverage(referenced_capture_groups_length_copy)));
            data_pairs.put("Median referenced capture group length", this.getFormattedValue(getMedian(referenced_capture_groups_length_copy)));
        }

        data_pairs.put("Fixed String Referenced Capture Groups", this.getFormattedValue((double) this.fixed_referenced_capture_group_lengths.size()));

        for (Entry<Character, Integer> entry : this.flag_occurrences.entrySet())
            data_pairs.put(entry.getKey().toString() + " flag", this.getFormattedValue(entry.getValue().doubleValue()));

        for (Entry<String, Integer> entry : this.total_operator_occurrences.entrySet())
            data_pairs.put(entry.getKey() + " total occurrences", this.getFormattedValue(entry.getValue().doubleValue()));

        for (Entry<String, Integer> entry : this.expression_operator_occurrences.entrySet())
            data_pairs.put(entry.getKey() + " expression occurrences", this.getFormattedValue(entry.getValue().doubleValue()));

        for (Entry<Integer, Integer> entry : this.bounded_quantifier_occurrences.entrySet())
            data_pairs.put(entry.getKey() + " repetitions quantifier occurences", this.getFormattedValue(entry.getValue().doubleValue()));

        return data_pairs;
    }

    private String getFormattedValue(Double value)
    {
        String format = value == value.intValue() ? "%.0f" : "%.4f";
        return String.format(format, value);
    }

    public void print()
    {
        Map<String, String> data = this.getData();
        for (Entry<String, String> data_entry : data.entrySet())
            System.out.println(data_entry.getKey() + ": " + data_entry.getValue());
    }
}
