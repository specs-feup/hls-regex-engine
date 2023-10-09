package pcreToHLS;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RulesAnalyzer {
    private Map<String, Integer> total_operator_occurrences;
    private Map<String, Integer> expression_operator_occurrences;
    private Map<Character, Integer> flag_occurrences;

    public RulesAnalyzer()
    {
        this.total_operator_occurrences = new HashMap<>();
        this.expression_operator_occurrences = new HashMap<>();
        this.flag_occurrences = new HashMap<>();
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

    public void add(RulesAnalyzer other)
    {
        other.total_operator_occurrences.forEach((key, value) -> this.total_operator_occurrences.merge(key, value, Integer::sum));
        other.expression_operator_occurrences.forEach((key, value) -> this.expression_operator_occurrences.merge(key, value, Integer::sum));
        other.flag_occurrences.forEach((key, value) -> this.flag_occurrences.merge(key, value, Integer::sum));
    }

    public void print()
    {
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
