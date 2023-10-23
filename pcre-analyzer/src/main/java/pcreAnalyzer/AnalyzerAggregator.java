package pcreAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.File;

import com.opencsv.CSVWriter;

public class AnalyzerAggregator {
    List<RulesAnalyzer> analyzers;

    public AnalyzerAggregator()
    {
        this.analyzers = new LinkedList<>();
    }

    public void addAnalyzer(RulesAnalyzer analyzer)
    {
        this.analyzers.add(analyzer);
    }

    private String[] getHeaders(List<Map<String, String>> data)
    {
        String[] headers = data.stream()
        .flatMap(map -> map.keySet().stream())
        .distinct()
        .toArray(String[]::new);

        return headers;
    }

    private List<String[]> getRows(String[] headers, List<Map<String, String>> data)
    {
        List<String[]> rows = new LinkedList<>();
        for (Map<String, String> row : data) 
        {
            String[] row_data = new String[headers.length];
            for (int i = 0; i < headers.length; i++)
                row_data[i] = row.getOrDefault(headers[i], "0");
            rows.add(row_data);
        }

        return rows;
    }

    private List<Map<String, String>> getAllFlagOccurrenceData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getFlagOccurrenceData());
        return data;
    }

    private List<Map<String, String>> getAllTotalOperatorOccurenceData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getTotalOperatorOccurenceData());
        return data;
    }

    private List<Map<String, String>> getAllExpressionOperatorOccurenceData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getExpressionOperatorOccurenceData());
        return data;
    }

    private List<Map<String, String>> getAllBoundedQuantifierRepetitionData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getBoundedQuantifierRepetitionData());
        return data;
    }

    private List<Map<String, String>> getAllExpressionLengthData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getExpressionLengthData());
        return data;
    }

    private List<Map<String, String>> getAllCaptureGroupLengthData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getCaptureGroupLengthData());
        return data;
    }

    private List<Map<String, String>> getAllReferencedCaptureGroupLengthData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getReferencedCaptureGroupLengthData());
        return data;
    }

    private List<Map<String, String>> getAllFixedReferencedCaptureGroupLengthData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getFixedReferencedCaptureGroupLengthData());
        return data;
    }

    private void exportData(String file_path, List<Map<String, String>> data)
    {
        String[] headers = this.getHeaders(data);
        List<String[]> rows = this.getRows(headers, data);

        try 
        {
            FileWriter writer = new FileWriter(file_path);
            CSVWriter csv_writer = new CSVWriter(writer);
            csv_writer.writeNext(headers);
            for (String[] row : rows)
                csv_writer.writeNext(row);

            csv_writer.close();
        } 
        catch (IOException e) 
        {
            System.err.println("Failed to export csv to " + file_path);
            System.exit(-1);
        }
    }

    public void export(String dir_path)
    {
        File directory = new File(dir_path);
        if (!directory.exists())
            directory.mkdir();

        this.exportData(dir_path + File.separator + "flag_report.csv", getAllFlagOccurrenceData());
        this.exportData(dir_path + File.separator + "total_operator_report.csv", getAllTotalOperatorOccurenceData());
        this.exportData(dir_path + File.separator + "expression_operator_report.csv", getAllExpressionOperatorOccurenceData());
        this.exportData(dir_path + File.separator + "bounded_quantifier_report.csv", getAllBoundedQuantifierRepetitionData());
        this.exportData(dir_path + File.separator + "expression_length_report.csv", getAllExpressionLengthData());
        this.exportData(dir_path + File.separator + "capture_group_length_report.csv", getAllCaptureGroupLengthData());
        this.exportData(dir_path + File.separator + "referenced_capture_group_length_report.csv", getAllReferencedCaptureGroupLengthData());
        this.exportData(dir_path + File.separator + "fixed_referenced_capture_group_length_report.csv", getAllFixedReferencedCaptureGroupLengthData());
    }
}
