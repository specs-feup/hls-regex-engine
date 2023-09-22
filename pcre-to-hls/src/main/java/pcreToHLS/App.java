package pcreToHLS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.cli.*;

public class App {

    public static void main(String[] args)
    {
        String generation_path = System.getProperty("user.home") + "\\Desktop\\generated.c";
        CommandLine cmd = parseOptions(args);
        boolean generate_dfas = cmd.hasOption("dfa");
        Map<String, String> expressions = getExpressions(cmd);

        CodeGenerator generator = new CodeGenerator(expressions, generate_dfas);
        System.out.println("\n === Analyzer ===");
        generator.getAnalyzer().print();
        generator.generate(generation_path);
        System.out.println("\nMatcher generated in " + generation_path);
    }

    private static CommandLine parseOptions(String[] args)
    {
        Options options = new Options();
        OptionGroup alternative_expression_source = new OptionGroup();
        Option file_option = new Option("f", "file", true, "parses pcre ruleset in 'resources/<arg>.pcre' file");
        Option dir_option = new Option("d", "dir", false, "parses all pcre rulesets in 'resources' directory");
        alternative_expression_source.addOption(file_option);
        alternative_expression_source.addOption(dir_option);
        options.addOptionGroup(alternative_expression_source);
        Option dfa_option = new Option("dfa", "uses DFAs instead of NFAs");
        options.addOption(dfa_option);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("pcreToHLS", options);
            System.exit(-1);
        }

        return cmd;
    }

    private static Map<String, String> getExpressions(CommandLine cmd)
    {
        String rules_path_name = "/rules";
        Map<String, String> expressions = new HashMap<>();

        if (cmd.hasOption("f")) 
        {
            String file_name = cmd.getOptionValue("f") + ".pcre";
            String rule_path = App.class.getResource(rules_path_name + "/" + file_name).getPath();
            expressions.putAll(getExpressionsFromFile(rule_path));
        } 
        else if (cmd.hasOption("d")) 
        {
            String rules_path = App.class.getResource(rules_path_name).getPath();
            File[] rule_files = new File(rules_path).listFiles();
            for (File rule_file : rule_files)
                expressions.putAll(getExpressionsFromFile(rule_file.getPath()));
        } 
        else 
        {
            Scanner scanner = new Scanner(System.in);
            String expr = scanner.nextLine();
            String pcre = expr.substring(expr.indexOf('/') + 1, expr.lastIndexOf('/'));
            String pcre_flags = expr.substring(expr.lastIndexOf('/') + 1);
            expressions.put(pcre, pcre_flags);
            scanner.close();
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
