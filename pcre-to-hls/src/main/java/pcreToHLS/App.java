package pcreToHLS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import jexer.TAction;
import jexer.TApplication;
import jexer.TButton;
import jexer.TCheckBox;
import jexer.TField;
import jexer.TKeypress;
import jexer.TLabel;
import jexer.TRadioButton;
import jexer.TRadioGroup;
import jexer.TWidget;
import jexer.TWindow;
import jexer.bits.ColorTheme;
import jexer.event.TKeypressEvent;

public class App extends TApplication {

    public static void main(String[] args) throws Exception
    {        
        (new App()).run();
    }

    public App() throws UnsupportedEncodingException
    {
        super(TApplication.BackendType.SWING);
        this.setColors();
        String generation_path = System.getProperty("user.home") + "\\Desktop\\generated.cpp";
        UnescapedWindow window = new UnescapedWindow(this, "PCRE to HLS", this.getDesktop().getWidth(), this.getDesktop().getHeight(), TWindow.NOCLOSEBOX + TWindow.NOZOOMBOX);
        TRadioGroup group = window.addRadioGroup(0, 0, "PCRE Source");

        for (ExpressionSource source : ExpressionSource.values())
        {
            TRadioButton radio_button = group.addRadioButton(source.toString());
            if (source.isDefault())
                radio_button.setSelected(true);
        }

        TCheckBox debug_check_box = window.addCheckBox(group.getWidth() + 1, 1, "Debug Mode", true);
        TCheckBox dfa_check_box = window.addCheckBox(group.getWidth() + 1, 2, "DFAs instead of NFAs", false);
        TCheckBox remove_unused_fifos_check_box = window.addCheckBox(group.getWidth() + 1, 3, "Transform unused capture groups into non-capture", false);
        TCheckBox expand_fixed_fifos_check_box = window.addCheckBox(group.getWidth() + 1, 4, "Expand constant group references", false);
        int input_y = group.getHeight() + 1;
        TLabel input_label = window.addLabel("Input:", 0, input_y);
        int input_x_offset = input_label.getWidth() + 1; 
        int input_length = window.getWidth() - input_x_offset - 6;
        TField input_field = window.addField(input_x_offset, input_y, input_length, false, "//");
        input_field.setPosition(1);

        int button_y = input_label.getY() + 2;
         TAction button_action = new TAction() {
            @Override
            public void DO() {
                String input_string = input_field.getText();
                ExpressionSource source = getExpressionSource(group);
                Map<String, String> expressions = getExpressions(source, input_string);
                CodeGenerator generator = new CodeGenerator(expressions, debug_check_box.isChecked(), dfa_check_box.isChecked(), remove_unused_fifos_check_box.isChecked(), expand_fixed_fifos_check_box.isChecked());
                generator.generate(generation_path);
                System.out.println("\nMatcher generated in " + generation_path);
            }
        };

        TButton generate_button = window.addButton("Generate", 0, button_y, button_action);
        generate_button.setActive(false);
        input_field.setActive(true);
    }

    @Override
    public void onExit()
    {
        System.out.println("Exiting...");
    }

    private void setColors()
    {
        int background_rgb = 0x000000;
        ColorTheme theme = this.getTheme();

        for (String attribute_name : theme.getColorNames())
            if (!attribute_name.contains("tfield") && !attribute_name.contains("tbutton"))
                theme.getColor(attribute_name).setBackColorRGB(background_rgb);
    }

    private class UnescapedWindow extends TWindow {
        public UnescapedWindow(TApplication app, String title, int width, int height, int flags) {
            super(app, title, width, height, flags);
        }

        @Override
        public void onKeypress(TKeypressEvent key_press_event) {
            TKeypress key_press = key_press_event.getKey();
            if (key_press.getChar() == 65535) 
            {
                char c = key_press.isShift() ? '^' : '~';
                key_press_event = new TKeypressEvent(getBackend(), new TKeypress(false, 0, c, false, false, false));
            }
            else if (key_press.getChar() >= 33 && key_press.getChar() <= 127)
                key_press_event = new TKeypressEvent(getBackend(), new TKeypress(false, 0, key_press.getChar(), false, false, false));
            else if (key_press.isCtrl() && key_press.getKeyCode() == TKeypress.BACKSPACE)
            {
                TWidget active_child = this.getActiveChild();
                if (active_child instanceof TField)
                    ((TField) active_child).setText("");
            }

            super.onKeypress(key_press_event);
        }
    }

    private enum ExpressionSource {
        STRING(1),
        FILE(2),
        DIRECTORY(3);

        private final String name;
        private final int id;

        private ExpressionSource(int id) {
            this.id = id;
            switch (id) {
                case 1:
                    this.name = "String (Input)";
                    break;
                case 2:
                    this.name = "File (Input)";
                    break;
                default:
                    this.name = "Directory";
                    break;
            }
        }

        public static ExpressionSource fromId(int id) {
            for (ExpressionSource source : values())
                if (source.id == id)
                    return source;

            throw new IllegalArgumentException("Invalid enum id: " + id);
        }

        @Override
        public String toString() {
            return this.name;
        }

        public boolean isDefault() {
            return this == ExpressionSource.STRING;
        }
    };

    private static ExpressionSource getExpressionSource(TRadioGroup group)
    {
        return ExpressionSource.fromId(group.getSelected());
    }

    private static Map<String, String> getExpressions(ExpressionSource source, String input_string)
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
                String file_name = input_string + ".pcre";
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
