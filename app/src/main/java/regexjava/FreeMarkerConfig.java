package regexjava;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public enum FreeMarkerConfig {
    INSTANCE();
    private String template_name = "/template.ftlh";
    private Configuration cfg;
  
    private FreeMarkerConfig()
    {
        // Create your Configuration instance, and specify if up to what FreeMarker
        // version (here 2.3.32) do you want to apply the fixes that are not 100%
        // backward-compatible. See the Configuration JavaDoc for details.
        this.cfg = new Configuration(Configuration.VERSION_2_3_32);

        // Specify the source where the template files come from. Here I set a
        // plain directory for it, but non-file-system sources are possible too:
        URL template_url = getClass().getResource(template_name);
        File templates_dir = new File(template_url.getPath()).getParentFile();
        try {
            cfg.setDirectoryForTemplateLoading(templates_dir);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // From here we will set the settings recommended for new projects. These
        // aren't the defaults for backward compatibilty.

        // Set the preferred charset template files are stored in. UTF-8 is
        // a good choice in most applications:
        cfg.setDefaultEncoding("UTF-8");

        // Sets how errors will appear.
        // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
        cfg.setLogTemplateExceptions(false);

        // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
        cfg.setWrapUncheckedExceptions(true);

        // Do not fall back to higher scopes when reading a null loop variable:
        cfg.setFallbackOnNullLoopVariable(false);
    }

    public Configuration getConfig()
    {
        return this.cfg;
    }

    public String getTemplateName()
    {
        return this.template_name;
    }
}
