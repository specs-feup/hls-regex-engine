package pcreAnalyzer;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private Map<Double, Integer> expression_lengths;
    private Map<Double, Integer> capture_group_lengths;
    private Map<Double, Integer> referenced_capture_group_lengths;
    private Map<Double, Integer> fixed_referenced_capture_group_lengths;

    public RulesAnalyzer(String name)
    {
        this.name = name;
        this.expression_no = 0;
        this.total_operator_occurrences = new HashMap<>();
        this.expression_operator_occurrences = new HashMap<>();
        this.flag_occurrences = new HashMap<>();
        this.bounded_quantifier_occurrences = new TreeMap<>();
        this.expression_lengths = new TreeMap<>();
        this.capture_group_lengths = new TreeMap<>();
        this.referenced_capture_group_lengths = new TreeMap<>();
        this.fixed_referenced_capture_group_lengths = new TreeMap<>();
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
        this.expression_lengths.merge(expression_length, 1, Integer::sum);
    }

    public void addCaptureGroupLength(double group_length)
    {
        this.capture_group_lengths.merge(group_length, 1, Integer::sum);
    }

    public void addReferencedCaptureGroupLength(Double referenced_group_length)
    {
        this.referenced_capture_group_lengths.merge(referenced_group_length, 1, Integer::sum);
    }

    public void addFixedReferencedCaptureGroupLength(Double fixed_referenced_group_length)
    {
        this.fixed_referenced_capture_group_lengths.merge(fixed_referenced_group_length, 1, Integer::sum);
    }

    public void add(RulesAnalyzer other)
    {
        this.expression_no += other.expression_no;
        other.expression_lengths.forEach((key, value) -> this.expression_lengths.merge(key, value, Integer::sum));
        other.capture_group_lengths.forEach((key, value) -> this.capture_group_lengths.merge(key, value, Integer::sum));
        other.referenced_capture_group_lengths.forEach((key, value) -> this.referenced_capture_group_lengths.merge(key, value, Integer::sum));
        other.fixed_referenced_capture_group_lengths.forEach((key, value) -> this.fixed_referenced_capture_group_lengths.merge(key, value, Integer::sum));
        other.total_operator_occurrences.forEach((key, value) -> this.total_operator_occurrences.merge(key, value, Integer::sum));
        other.expression_operator_occurrences.forEach((key, value) -> this.expression_operator_occurrences.merge(key, value, Integer::sum));
        other.flag_occurrences.forEach((key, value) -> this.flag_occurrences.merge(key, value, Integer::sum));
        other.bounded_quantifier_occurrences.forEach((key, value) -> this.bounded_quantifier_occurrences.merge(key, value, Integer::sum));
    }

    private <T> Map<String, String> getData(String header_append, Map<T, Integer> data)
    {
        Map<String, String> data_pairs = new LinkedHashMap<>();
        data_pairs.put("Ruleset", this.name);
        data_pairs.put("Number of Expressions", Integer.toString(this.expression_no));
        for (Entry<T, Integer> entry : data.entrySet())
            data_pairs.put(entry.getKey().toString() + " " + header_append, Integer.toString(entry.getValue()));
        return data_pairs;
    }

    public Map<String, String> getFlagOccurrenceData()
    {
        return this.getData("flag", this.flag_occurrences);
    }

    public Map<String, String> getTotalOperatorOccurenceData()
    {
        return this.getData("total occurrences", this.total_operator_occurrences);
    }

    public Map<String, String> getExpressionOperatorOccurenceData()
    {
        return this.getData("expression occurrences", this.expression_operator_occurrences);
    }

    public Map<String, String> getBoundedQuantifierRepetitionData()
    {
        return this.getData("repetitions quantifier occurrences", this.bounded_quantifier_occurrences);
    }

    public Map<String, String> getExpressionLengthData()
    {
        return this.getData("length expressions", this.expression_lengths);
    }

    public Map<String, String> getCaptureGroupLengthData()
    {
        return this.getData("length capture groups", this.capture_group_lengths);
    }

    public Map<String, String> getReferencedCaptureGroupLengthData()
    {
        return this.getData("length referenced capture groups", this.referenced_capture_group_lengths);
    }

    public Map<String, String> getFixedReferencedCaptureGroupLengthData()
    {
        return this.getData("length fixed referenced capture groups", this.fixed_referenced_capture_group_lengths);
    }
}
