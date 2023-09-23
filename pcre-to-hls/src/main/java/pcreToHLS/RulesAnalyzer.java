package pcreToHLS;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RulesAnalyzer {
    private Map<String, Integer> operator_occurrences;
    private Map<Character, Integer> flag_occurrences;

    private RulesAnalyzer(Map<String, Integer> operator_occurences, Map<Character, Integer> flag_occurrences)
    {
        this.operator_occurrences = operator_occurences;
        this.flag_occurrences = flag_occurrences;
    }

    public RulesAnalyzer()
    {
        this(new HashMap<>(), new HashMap<>());
    }

    public void addOperatorOccurrence(String operator)
    {
        this.operator_occurrences.merge(operator, 1, Integer::sum);
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

    public void print()
    {
        System.out.println("PCRE Flags: ");
        for (Entry<Character, Integer> entry : this.flag_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": " + entry.getValue());

        System.out.println("PCRE Operators: ");
        for (Entry<String, Integer> entry : this.operator_occurrences.entrySet())
            System.out.println("  -" + entry.getKey() + ": " + entry.getValue());
    }
}
