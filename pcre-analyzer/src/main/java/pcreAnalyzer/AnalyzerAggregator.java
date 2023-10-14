package pcreAnalyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    public void print()
    {
        for (RulesAnalyzer analyzer : this.analyzers)
            analyzer.print();
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
                row_data[i] = row.getOrDefault(headers[i], "");
            rows.add(row_data);
        }

        return rows;
    }

    private List<Map<String, String>> getTotalData()
    {
        List<Map<String, String>> data = new LinkedList<>();
        for (RulesAnalyzer analyzer : this.analyzers)
            data.add(analyzer.getData());
        return data;
    }

    public void export(String file_path)
    {
        List<Map<String, String>> data = getTotalData();
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
}
