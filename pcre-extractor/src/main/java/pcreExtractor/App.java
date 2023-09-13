package pcreExtractor;

import java.io.File;  
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
  public static void main(String[] args) 
  {
    String rules_path = App.class.getResource("/rules").getPath();
    File[] rule_files = new File(rules_path).listFiles();
    Pattern pattern = Pattern.compile("pcre:\\\"\\/(.*?)/([^/]*?)\\\";");
    for (File rule_file : rule_files)
    {
      String ruleset_name = rule_file.getName();
      String out_path = System.getProperty("user.home") + "\\Desktop\\" + ruleset_name + ".txt";

       try {
        BufferedReader reader = new BufferedReader(new FileReader(rule_file));
        FileWriter writer = new FileWriter(out_path);
        String line;
        while ((line = reader.readLine()) != null) 
        {
          Matcher matcher = pattern.matcher(line);
          while (matcher.find()) {
            String pcre_string = matcher.group(1);
            String pcre_options = matcher.group(2);
            writer.write("/" + pcre_string + "/" + pcre_options + "\n");
          }
        }
        reader.close();
        writer.close();
      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }
}
