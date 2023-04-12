package regexjava;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import regexjava.TemplateAtoms.User;

public class CodeGenerator {
    private NFA automata;

    public CodeGenerator(String raw_regex)
    {
        this.automata = new NFA(raw_regex);
    }

    public void generate()
    {
        Map<String, Object> root = new HashMap<>();
        User user = new User();
        user.setName("Pedro");
        user.setSurname("Alves");
        root.put("user", user);
        try {
            Template template = FreeMarkerConfig.INSTANCE.getConfig().getTemplate(FreeMarkerConfig.INSTANCE.template_name);
            Writer out = new OutputStreamWriter(System.out);
            template.process(root, out);
        } catch (IOException | TemplateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
