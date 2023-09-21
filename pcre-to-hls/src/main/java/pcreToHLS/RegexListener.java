// Generated from regexParser.g4 by ANTLR 4.12.0
package pcreToHLS;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import org.antlr.v4.runtime.tree.TerminalNode;

import PCREgrammar.PCREgrammarBaseListener;
import PCREgrammar.PCREgrammarParser.AlternationContext;
import PCREgrammar.PCREgrammarParser.AtomContext;
import PCREgrammar.PCREgrammarParser.BackreferenceContext;
import PCREgrammar.PCREgrammarParser.CalloutContext;
import PCREgrammar.PCREgrammarParser.CaptureContext;
import PCREgrammar.PCREgrammarParser.Cc_atomContext;
import PCREgrammar.PCREgrammarParser.Cc_literalContext;
import PCREgrammar.PCREgrammarParser.Character_classContext;
import PCREgrammar.PCREgrammarParser.ConditionalContext;
import PCREgrammar.PCREgrammarParser.ExprContext;
import PCREgrammar.PCREgrammarParser.LiteralContext;
import PCREgrammar.PCREgrammarParser.Look_aroundContext;
import PCREgrammar.PCREgrammarParser.Non_captureContext;
import PCREgrammar.PCREgrammarParser.ParseContext;
import PCREgrammar.PCREgrammarParser.QuantifierContext;
import PCREgrammar.PCREgrammarParser.Quantifier_typeContext;
import PCREgrammar.PCREgrammarParser.Shared_atomContext;
import PCREgrammar.PCREgrammarParser.Shared_literalContext;
import PCREgrammar.PCREgrammarParser.Subroutine_referenceContext;

public class RegexListener extends PCREgrammarBaseListener {

    private Stack<EpsilonNFA> stack;
    private RulesAnalyzer analyzer;

    public RegexListener(RulesAnalyzer analyzer)
    {
        this.stack = new Stack<>();
        this.analyzer = analyzer;
    }

    private void alternate()
    {
        EpsilonNFA second = stack.pop();
        EpsilonNFA first = stack.pop();
        stack.push(EpsilonNFA.join(first, second));
    }

    private void concat()
    {
        EpsilonNFA second = stack.pop();
        EpsilonNFA first = stack.pop();
        stack.push(EpsilonNFA.concat(first, second));
    }

    public void enterAtom(AtomContext ctx)
    {
        LiteralContext literal = ctx.literal();
        Character_classContext character_class = ctx.character_class();
        Shared_atomContext shared_atom = ctx.shared_atom();
        TerminalNode dot = ctx.Dot();
        if (literal != null)
            proccessLiteral(literal);
        else if (character_class != null)
            processCharacter_class(character_class);
        else if (shared_atom != null)
        {
            AtomicBoolean negated = new AtomicBoolean(false);
            Set<Integer> code_points = new HashSet<>();
            code_points.addAll(getShared_atomCodePoints(shared_atom, negated));

            if (!code_points.isEmpty())
                stack.push(new EpsilonNFA(new CharacterClassEdge(code_points, negated.get())));
        }
        else if (dot != null)
            stack.push(new EpsilonNFA(new WildcardEdge()));
        else if (ctx.Caret() != null || ctx.StartOfSubject() != null)
            stack.push(new EpsilonNFA(new StartAnchorEdge()));
        else if (ctx.EndOfSubjectOrLine() != null || ctx.EndOfSubjectOrLineEndOfSubject() != null)
            stack.push(new EpsilonNFA(new EndAnchorEdge())); 
    }

    private boolean isEscapedChar(Shared_literalContext ctx)
    {
        String[] escape_chars = {"\\a", "\\e", "\\f", "\\n", "\\r", "\\t"};
        return Arrays.asList(escape_chars).contains(ctx.getText());
    }

    private List<Integer> getShared_literalCodePoints(Shared_literalContext ctx)
    {
        String text = ctx.getText();
        if (text.length() == 1)
            return Arrays.asList((int)text.charAt(0));
        
        TerminalNode octal = ctx.OctalChar();
        TerminalNode hex = ctx.HexChar();
        TerminalNode quoted = ctx.Quoted();
        TerminalNode block_quoted = ctx.BlockQuoted();

        if (isEscapedChar(ctx))
            return Arrays.asList(getEscapedCharCodePoint(ctx.getText()));
        if (octal != null)
            return Arrays.asList(getOctalCharCodePoint(octal.getText()));
        if (hex != null)
            return Arrays.asList(getHexCharCodePoint(hex.getText()));
        if (quoted != null)
            return Arrays.asList(getQuotedCodePoint(quoted.getText()));

        return Arrays.asList(getBlockQuotedCodePoints(block_quoted.getText()));
    }

    private void proccessLiteral(LiteralContext ctx)
    {
        Shared_literalContext shared_literal = ctx.shared_literal();
        LabeledEdge<?> transition;
        if (shared_literal != null)
        {
            List<Integer> code_points = getShared_literalCodePoints(shared_literal);
            if (code_points.size() == 1)
                transition = new CharacterEdge(code_points.get(0));
            else 
                transition = new CharacterBlockEdge(code_points.toArray(new Integer[]{}));
        }
        else 
            transition = new CharacterEdge(']');

        stack.push(new EpsilonNFA(transition));
    }

    private int getEscapedCharCodePoint(String escaped_str)
    {
        char symbol = escaped_str.charAt(1);
        String unescaped_char = "";
        switch (symbol)
        {
            case 'a':
                unescaped_char = "\u0007";
                break;
            case 'e':
                unescaped_char = "\u001B";
                break;
            case 'f':
                unescaped_char = "\f";
                break;
            case 'n':
                unescaped_char = "\n";
                break;
            case 'r':
                unescaped_char = "\r";
                break;
            case 't':
                unescaped_char = "\t";
                break;
        }
        
        return unescaped_char.codePointAt(0);
    }

    private int getOctalCharCodePoint(String octal_str)
    {
        String unescaped_octal = octal_str.substring(1);
        return Integer.parseInt(unescaped_octal, 8);
    }

    private int getHexCharCodePoint(String hex_str)
    {
        String unescaped_hex = hex_str.substring(2);
        if (unescaped_hex.contains("{"))
            unescaped_hex = unescaped_hex.substring(1, unescaped_hex.length() - 1);

        int code_point = Integer.parseInt(unescaped_hex, 16);

        return code_point;
    }

    private int getQuotedCodePoint(String quoted_str)
    {
        String unescaped_literal = quoted_str.substring(1);
        return unescaped_literal.codePointAt(0);
    }

    private Integer[] getBlockQuotedCodePoints(String block_quoted_str)
    {
        String unescaped_literal = block_quoted_str.substring(2, block_quoted_str.length() - 2);
        int[] primitive_arr = unescaped_literal.codePoints().toArray();
        Integer[] object_arr = new Integer[primitive_arr.length];
        Arrays.setAll(object_arr, i -> primitive_arr[i]);
        return object_arr;
    }

    private void processCharacter_class(Character_classContext ctx)
    {
        try {
            if (ctx.getText().length() < 3)
                throw new Exception("Invalid character class");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        AtomicBoolean negated = new AtomicBoolean(ctx.getText().charAt(1) == '^');
        List<Cc_atomContext> cc_atoms = ctx.cc_atom();
        Set<Integer> processed_atoms = new HashSet<>();
        for (int i = 0; i < cc_atoms.size(); i++)
        {
            List<Cc_literalContext> cc_literals = cc_atoms.get(i).cc_literal();
            Shared_atomContext shared_atom = cc_atoms.get(i).shared_atom();
            if (!cc_literals.isEmpty())
                processed_atoms.addAll(getCc_literalListCodePoints(cc_literals));
            else if (shared_atom != null)
                processed_atoms.addAll(getShared_atomCodePoints(shared_atom, negated));
        }
        
        if (!processed_atoms.isEmpty())
            stack.push(new EpsilonNFA(new CharacterClassEdge(processed_atoms, negated.get())));
 
    }

    private List<Integer> getCodePointsInRange(int start, int end)
    {
        List<Integer> code_points = new LinkedList<>();
        for (int i = start; i <= end; i++)
            code_points.add(i);
        return code_points;
    }

    private List<Integer> alpha()
    {
        List<Integer> lower = getCodePointsInRange('a', 'z');
        List<Integer> upper = getCodePointsInRange('A', 'Z');
        List<Integer> alpha = new LinkedList<>(lower);
        alpha.addAll(upper);
        return alpha;
    }

    private List<Integer> digit()
    {
        return getCodePointsInRange('0', '9');
    }

    private List<Integer> horizontalWhiteSpace()
    {
        return Arrays.asList((int) '\t', (int) ' ');
    }

    private List<Integer> verticalWhiteSpace()
    {
        return Arrays.asList((int) '\r', (int) '\n', 0x000C, 0x000B, 0x0085, 0x2028, 0x2029);
    }

    private List<Integer> whiteSpace()
    {
        List<Integer> horizontal_space = horizontalWhiteSpace();
        List<Integer> vertical_space = verticalWhiteSpace();
        List<Integer> white_space = new LinkedList<>(horizontal_space);
        white_space.addAll(vertical_space);
        return white_space;
    }

    private List<Integer> word()
    {
        List<Integer> alpha = alpha();
        List<Integer> digit = digit();
        List<Integer> word = new LinkedList<>(alpha);
        word.addAll(digit);
        word.add((int) '_');
        return word;
    }

    private void processNewLineSequence() 
    {
        stack.push(new EpsilonNFA(new CharacterBlockEdge(new Integer[]{(int)'\r',(int)'\n'})));
        stack.push(new EpsilonNFA(new CharacterClassEdge(new HashSet<>(Arrays.asList((int)'\r', (int)'\n', (int)'\f', 0x000B, 0x0085)), false)));
        alternate();
    }

    private List<Integer> getShared_atomCodePoints(Shared_atomContext ctx, AtomicBoolean negated)
    {
        if (ctx.POSIXNamedSet() != null)
            return getPosixSetsCodePoints(ctx.POSIXNamedSet(), false);

        if (ctx.POSIXNegatedNamedSet() != null)
        {
            negated.set(!negated.get());
            return getPosixSetsCodePoints(ctx.POSIXNegatedNamedSet(), true);
        }

        if (ctx.ControlChar() != null)
            return getControlCharCodePoints(ctx.ControlChar());
        
        if (ctx.DecimalDigit() != null)
            return digit();

        if (ctx.NotDecimalDigit() != null)
        {
            negated.set(!negated.get());
            return digit();
        }

        if (ctx.HorizontalWhiteSpace() != null)
            return horizontalWhiteSpace();

        if (ctx.NotHorizontalWhiteSpace() != null)
        {
            negated.set(!negated.get());
            return horizontalWhiteSpace();
        }

        if (ctx.NewLineSequence() != null)
            processNewLineSequence();

        if (ctx.NotNewLine() != null)
        {
            negated.set(!negated.get());
            return Arrays.asList((int)'\n');
        }

        if (ctx.WhiteSpace() != null)
            return whiteSpace();

        if (ctx.NotWhiteSpace() != null)
        {
            negated.set(!negated.get());
            return whiteSpace();
        }

        if (ctx.VerticalWhiteSpace() != null)
            return verticalWhiteSpace();

        if (ctx.NotVerticalWhiteSpace() != null)
        {
            negated.set(!negated.get());
            return verticalWhiteSpace();
        }

        if (ctx.WordChar() != null)
            return word();

        if (ctx.NotWordChar() != null)
        {
            negated.set(!negated.get());
            return word();
        }

        if (ctx.Backslash() != null)
        {
            return Arrays.asList(0);
        }

        return Arrays.asList();
    }

    private List<Integer> getControlCharCodePoints(TerminalNode control_node) // https://www.pcre.org/original/doc/html/pcrepattern.html#SEC5 \cx
    {
        char character = Character.toUpperCase(control_node.getText().charAt(2));

        try {
            if (character >= 0 && character <= 127) // ascii
                throw new Exception("Invalid control character");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        BigInteger code = new BigInteger(Integer.toString(character));
        BigInteger flipped_code = code.flipBit(6);
        return Arrays.asList(flipped_code.intValueExact());
    }

    private List<Integer> getPosixSetsCodePoints(TerminalNode posix_node, boolean negated)
    {
        String text = posix_node.getText();
        int name_offset = negated ? 2 : 1;
        String set_name = text.substring(text.indexOf(':') + name_offset, text.lastIndexOf(':'));
        List<Integer> code_points = new LinkedList<>();

        switch (set_name)
        {
            case "alnum": // alphanumeric
                code_points.addAll(alpha());
                code_points.addAll(digit());
                break;
            case "alpha": //alphabetic
                code_points.addAll(alpha());
                break;
            case "ascii":       // 0-127
                code_points.addAll(getCodePointsInRange(0, 127));
                break;
            case "blank":       // space or tab
                code_points.addAll(horizontalWhiteSpace());
                break;
            case "cntrl":       // control character
                code_points.addAll(getCodePointsInRange(0x0000, 0x001F));
                code_points.add(0x007F);
                break;
            case "digit":       // decimal digit
                code_points.addAll(digit());
                break;
            case "graph":       // printing, excluding space
                code_points.addAll(getCodePointsInRange(0x0021, 0x007E));
                break;
            case "lower":       // lower case letter
                code_points.addAll(getCodePointsInRange('a', 'z'));
                break;
            case "print":       // printing, including space
                code_points.addAll(getCodePointsInRange(0x0020, 0x007E));
                break;
            case "punct":       // printing, excluding alphanumeric [!"#$%&'()*+,\-./:;<=>?@[]^_`{|}~]
                List<Integer> print = getCodePointsInRange(0x0020, 0x007E);
                List<Integer> alpha_num = new LinkedList<>(alpha());
                alpha_num.addAll(digit());
                code_points = print;
                code_points.removeAll(alpha_num);
                break;
            case "space":       // white space
                code_points.addAll(whiteSpace());
                break;
            case "upper":       // upper case letter
                code_points.addAll(getCodePointsInRange('A', 'Z'));
                break;
            case "word":        // same as \w
                code_points.addAll(word());
                break;
            case "xdigit":      // hexadecimal digit
                code_points.addAll(digit());
                code_points.addAll(getCodePointsInRange('a', 'f'));
                code_points.addAll(getCodePointsInRange('A', 'F'));
                break;
            default:
                try {
                    throw new Exception("Invalid posix named set");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
        }

        return code_points;
    }

    private List<Integer> getCc_literalListCodePoints(List<Cc_literalContext> cc_literals)
    {
        List<Integer> code_points;
        if (cc_literals.size() == 1) // no hyphen
            code_points = getCc_literalCodePoints(cc_literals.get(0));
        else // hyphen 
        {
            List<Integer> first_code_points = getCc_literalCodePoints(cc_literals.get(0));
            List<Integer> second_code_points = getCc_literalCodePoints(cc_literals.get(1));
            code_points = new LinkedList<>(first_code_points);
            code_points.addAll(second_code_points);
            code_points.addAll(getCodePointsInRange(first_code_points.get(first_code_points.size() - 1), second_code_points.get(0)));
        }

        return code_points;
    }

    private List<Integer> getCc_literalCodePoints(Cc_literalContext ctx)
    {
        Shared_literalContext shared_literal = ctx.shared_literal();
        if (shared_literal != null)
            return getShared_literalCodePoints(shared_literal);
        if (ctx.WordBoundary() != null)
            return Arrays.asList("\u0008".codePointAt(0));
        
        return Arrays.asList(ctx.getText().codePointAt(0));
    }

    public void exitExpr(ExprContext ctx)
    {
        for(int i = 0; i < ctx.element().size() - 1; i++)
        {
            this.analyzer.addOccurence("Concatenations");
            concat();
        }
    }

    public void exitAlternation(AlternationContext ctx)
    {
        for(int i = 0; i < ctx.Pipe().size(); i++)
        {
            this.analyzer.addOccurence("Alternations");
            alternate();
        }
    }

    public void enterQuantifier(QuantifierContext ctx)
    {
        this.analyzer.addOccurence("Total Quantifiers");
        EpsilonNFA top = stack.pop();

        if (ctx.Plus() != null) // +
            stack.push(EpsilonNFA.oneOrMore(top)); 
        else if (ctx.QuestionMark() != null) // ?
            stack.push(EpsilonNFA.zeroOrOne(top));
        else if (ctx.Star() != null) // *
            stack.push(EpsilonNFA.zeroOrMore(top));
        else if (ctx.OpenBrace() != null)
        {
            this.analyzer.addOccurence("Bounded Quantifiers");
            processBoundedQuantifier(ctx, top);
        }
    }

    private void processBoundedQuantifier(QuantifierContext ctx, EpsilonNFA top) 
    {
        if (ctx.number().size() == 1) 
        {
            int repetitions = Integer.parseInt(ctx.number(0).getText());
            if (ctx.Comma() != null)
                stack.push(EpsilonNFA.repeatAtLeast(top, repetitions));
            else
                stack.push(EpsilonNFA.repeatExactly(top, repetitions));
        } 
        else {
            int min_repetitions = Integer.parseInt(ctx.number(0).getText());
            int max_repetitions = Integer.parseInt(ctx.number(1).getText());
            stack.push(EpsilonNFA.repeatRange(top, min_repetitions, max_repetitions));
        }
    }

    public EpsilonNFA getEpsilonNFA()
    {
        EpsilonNFA top = this.stack.pop();
        EpsilonNFA padded_start = EpsilonNFA.concat(EpsilonNFA.zeroOrMore(new EpsilonNFA(new WildcardEdge())), top);
        return EpsilonNFA.concat(padded_start, EpsilonNFA.zeroOrMore(new EpsilonNFA(new WildcardEdge())));
    }

    // ==== ANALYZER ONLY ==== ANALYZER ONLY ==== ANALYZER ONLY ==== ANALYZER ONLY ==== ANALYZER ONLY ==== ANALYZER ONLY ====
    public void enterParse(ParseContext ctx)
    {
        this.analyzer.addOccurence("Expressions");
    }

    public void enterCharacter_class(Character_classContext ctx)
    {
        this.analyzer.addOccurence("Character Classes");
    }

    public void enterQuantifier_type(Quantifier_typeContext ctx)
    {
        if (ctx.Plus() != null)
            this.analyzer.addOccurence("Possessive Quantifiers");
        else if (ctx.QuestionMark() != null)
            this.analyzer.addOccurence("Lazy Quantifiers");
    }

    public void enterBackreference(BackreferenceContext ctx)
    {
        this.analyzer.addOccurence("Backreferences");
    }

    public void enterCapture(CaptureContext ctx)
    {
        this.analyzer.addOccurence("Capture Groups");
    }

    public void enterNon_capture(Non_captureContext ctx)
    {
        this.analyzer.addOccurence("Non-Capture Groups");
    }

    public void enterLook_around(Look_aroundContext ctx)
    {
        this.analyzer.addOccurence("LookArounds");
    }

    public void enterSubroutine_reference(Subroutine_referenceContext ctx)
    {
        this.analyzer.addOccurence("Subroutines");
    }

    public void enterConditional(ConditionalContext ctx)
    {
        this.analyzer.addOccurence("Conditional Patterns");
    }

    public void enterCallout(CalloutContext ctx)
    {
        this.analyzer.addOccurence("Callouts");
    }
    
}