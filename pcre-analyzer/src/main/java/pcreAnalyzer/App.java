package pcreAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
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

        RulesAnalyzer analyzer = new RulesAnalyzer();
        Map<String, String> expressions = getExpressions(source, cmd, input_string);
        for (Entry<String, String> expression : expressions.entrySet())
        {
            String pcre = expression.getKey();
            String flags = expression.getValue();
            analyzer.addFlagsOccurrence(flags);
            
            CharStream stream = CharStreams.fromString(pcre);
            PCREgrammarLexer lexer = new PCREgrammarLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PCREgrammarParser parser = new PCREgrammarParser(tokens);
            ParseTree tree = parser.parse();
            RulesAnalyzer local_analyzer = new RulesAnalyzer();

            try {
                RegexListener listener = new RegexListener(local_analyzer, flags);
                ParseTreeWalker walker = new ParseTreeWalker();
                walker.walk(listener, tree);
            } catch (Exception e) {
                System.out.println("Failed to parse: " + pcre + ". Error: " + e.getMessage());
            }
            
            analyzer.add(local_analyzer);
        }
        System.out.println("==== ANALYZER REPORT ====");
        analyzer.print();
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

    private static Map<String, String> getExpressions(ExpressionSource source, CommandLine cmd, String input_string)
    {
        String rules_path_name = "/rules";
        Map<String, String> expressions = new HashMap<>();

        switch(source)
        {
            case STRING:
                String pcre = input_string.substring(input_string.indexOf('/') + 1, input_string.lastIndexOf('/'));
                String pcre_flags = input_string.substring(input_string.lastIndexOf('/') + 1);
                expressions.put(pcre, pcre_flags);
                break;
            case FILE:
                String file_name = cmd.getOptionValue("f") + ".pcre";
                String rule_path = App.class.getResource(rules_path_name + "/" + file_name).getPath();
                expressions.putAll(getExpressionsFromFile(rule_path));
                break;
            case DIRECTORY:
                String rules_path = App.class.getResource(rules_path_name).getPath();
                File[] rule_files = new File(rules_path).listFiles();
                for (File rule_file : rule_files)
                    expressions.putAll(getExpressionsFromFile(rule_file.getPath()));
                break;
            default:
                break;
        }

        return expressions;
    }

    private static Map<String, String> getExpressionsFromFile(String file_path)
    {
        Map<String, String> expressions = new HashMap<>();
        File rule_file = new File(file_path);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(rule_file));
            String line;
            while ((line = reader.readLine()) != null) {
                String pcre = line.substring(line.indexOf('/') + 1, line.lastIndexOf('/'));
                String pcre_flags = line.substring(line.lastIndexOf('/') + 1);
                expressions.put(pcre, pcre_flags);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return expressions;
    }
    
}
