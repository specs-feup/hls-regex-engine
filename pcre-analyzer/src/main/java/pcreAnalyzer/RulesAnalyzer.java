package pcreAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opencsv.CSVWriter;

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

    private Map<String, Double> getDataPairs()
    {
        Map<String, Double> data_pairs = new LinkedHashMap<>();
        data_pairs.put("Expressions with unknown length", (double) removeInfinities(this.expression_lengths));
        if (!expression_lengths.isEmpty())
        {
            data_pairs.put("Average expression length", getAverage(this.expression_lengths));
            data_pairs.put("Median expression length", getMedian(this.expression_lengths));
        }

        data_pairs.put("Capture Groups with unknown Length", (double) removeInfinities(this.capture_group_lengths));
        if (!capture_group_lengths.isEmpty())
        {
            data_pairs.put("Average capture group length", getAverage(this.capture_group_lengths));
            data_pairs.put("Median capture group length", getMedian(this.capture_group_lengths));
        }

        for (Entry<Character, Integer> entry : this.flag_occurrences.entrySet())
            data_pairs.put(entry.getKey().toString() + " flag", entry.getValue().doubleValue());

        for (Entry<String, Integer> entry : this.total_operator_occurrences.entrySet())
            data_pairs.put(entry.getKey() + " total occurrences", entry.getValue().doubleValue());

        for (Entry<String, Integer> entry : this.expression_operator_occurrences.entrySet())
            data_pairs.put(entry.getKey() + " expression occurrences", entry.getValue().doubleValue());

        return data_pairs;
    }

    private String getFormattedValue(Entry<String, Double> data_entry)
    {
        String format = data_entry.getValue() == data_entry.getValue().intValue() ? "%.0f" : "%.4f";
        return String.format(format, data_entry.getValue());
    }

    public void export(String file_path)
    {
        Map<String, Double> data_pairs = this.getDataPairs();
        try 
        {
            FileWriter writer = new FileWriter(file_path);
            CSVWriter csv_writer = new CSVWriter(writer);
            for (Entry<String, Double> data_entry : data_pairs.entrySet())
            {
                String value =  this.getFormattedValue(data_entry);
                String[] data = {data_entry.getKey(), value};
                csv_writer.writeNext(data);
            }
            csv_writer.close();
            writer.close();
        } 
        catch (IOException e) 
        {
            System.err.print("Failed to export to " + file_path);
            System.exit(-1);
        }
    }

    public void print()
    {
        Map<String, Double> data = this.getDataPairs();
        for (Entry<String, Double> data_entry : data.entrySet())
        {
            String value =  this.getFormattedValue(data_entry);
            System.out.println(data_entry.getKey() + ": " + value);
        }
    }
}
