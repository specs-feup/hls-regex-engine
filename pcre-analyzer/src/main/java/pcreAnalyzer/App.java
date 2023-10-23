package pcreAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import PCREgrammar.PCREgrammarLexer;
import PCREgrammar.PCREgrammarParser;


public class App {

    private static class RegExp {
        public String flags;
        public String expression;

        public RegExp(String flags, String expression) {
            this.flags = flags;
            this.expression = expression;
        }
    }

    public static void main(String args[])
    {
        CommandLine cmd = parseOptions(args);
        ExpressionSource source = getExpressionSource(cmd);
        String input_string = "";
        if (source == ExpressionSource.STRING)
        {
            Scanner scanner = new Scanner(System.in);
            input_string = scanner.nextLine();
            scanner.close();
        }

        Map<String, List<RegExp>> expressions = getExpressions(source, cmd, input_string);
        AnalyzerAggregator aggregator = new AnalyzerAggregator();
        for (Entry<String, List<RegExp>> ruleset_expressions : expressions.entrySet())
        {
            String ruleset_name = ruleset_expressions.getKey();
            RulesAnalyzer analyzer = new RulesAnalyzer(ruleset_name);
            for (RegExp pcre : ruleset_expressions.getValue())
            {
                analyzer.addFlagsOccurrence(pcre.flags);
                CharStream stream = CharStreams.fromString(pcre.expression);
                PCREgrammarLexer lexer = new PCREgrammarLexer(stream);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                PCREgrammarParser parser = new PCREgrammarParser(tokens);
                ParseTree tree = parser.parse();
                RulesAnalyzer local_analyzer = new RulesAnalyzer();

                try {
                    RegexListener listener = new RegexListener(local_analyzer, pcre.flags);
                    ParseTreeWalker walker = new ParseTreeWalker();
                    walker.walk(listener, tree);
                } catch (Exception e) {
                    System.out.println("Failed to parse: " + pcre.expression + ". Error: " + e.getMessage());
                    e.printStackTrace();
                }
                
                analyzer.add(local_analyzer);
            }
            aggregator.addAnalyzer(analyzer);
        }

        String report_path = System.getProperty("user.home") + "\\Desktop\\rulesets_report";
        System.out.println("Generated rulesets report in " + report_path);
        aggregator.export(report_path);
    }

    private enum ExpressionSource { STRING, FILE, DIRECTORY};

    private static CommandLine parseOptions(String args[])
    {
        Options options = new Options();
        OptionGroup alternative_expression_source = new OptionGroup();
        Option file_option = new Option("f", "file", true, "parses pcre ruleset in file");
        Option dir_option = new Option("d", "dir", false, "parses pcre rulesets in directory");
        alternative_expression_source.addOption(file_option);
        alternative_expression_source.addOption(dir_option);
        options.addOptionGroup(alternative_expression_source);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } 
        catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("pcreAnalyzer", options);
            System.exit(-1);
        }
        return cmd;
    }


    private static ExpressionSource getExpressionSource(CommandLine cmd)
    {
        if (cmd.hasOption("f"))
            return ExpressionSource.FILE;
        else if (cmd.hasOption("d"))
            return ExpressionSource.DIRECTORY;
        else 
            return ExpressionSource.STRING;
    }

    private static Map<String, List<RegExp>> getExpressions(ExpressionSource source, CommandLine cmd, String input_string)
    {
        String rules_path_name = "/rules";
        Map<String, List<RegExp>> expressions = new HashMap<>();

        switch(source)
        {
            case STRING:
                String pcre = input_string.substring(input_string.indexOf('/') + 1, input_string.lastIndexOf('/'));
                String pcre_flags = input_string.substring(input_string.lastIndexOf('/') + 1);
                expressions.put("String", Arrays.asList(new RegExp(pcre_flags, pcre)));
                break;
            case FILE:
                String file_name = cmd.getOptionValue("f") + ".pcre";
                String rule_path = App.class.getResource(rules_path_name + "/" + file_name).getPath();
                expressions.put(cmd.getOptionValue("f"), getExpressionsFromFile(rule_path));
                break;
            case DIRECTORY:
                String rules_path = App.class.getResource(rules_path_name).getPath();
                File[] rule_files = new File(rules_path).listFiles();
                for (File rule_file : rule_files)
                    expressions.put(rule_file.getName(), getExpressionsFromFile(rule_file.getPath()));
                break;
            default:
                break;
        }

        return expressions;
    }

    private static List<RegExp> getExpressionsFromFile(String file_path)
    {
        List<RegExp> expressions = new LinkedList<>();
        File rule_file = new File(file_path);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(rule_file));
            String line;
            while ((line = reader.readLine()) != null) {
                String pcre = line.substring(line.indexOf('/') + 1, line.lastIndexOf('/'));
                String pcre_flags = line.substring(line.lastIndexOf('/') + 1);
                expressions.add(new RegExp(pcre_flags, pcre));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expressions;
    }
    
}
