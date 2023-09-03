// Generated from regexParser.g4 by ANTLR 4.12.0
package regexjava;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.tree.TerminalNode;

import PCREgrammar.PCREgrammarBaseListener;
import PCREgrammar.PCREgrammarParser.AlternationContext;
import PCREgrammar.PCREgrammarParser.AtomContext;
import PCREgrammar.PCREgrammarParser.Cc_atomContext;
import PCREgrammar.PCREgrammarParser.Cc_literalContext;
import PCREgrammar.PCREgrammarParser.Character_classContext;
import PCREgrammar.PCREgrammarParser.ExprContext;
import PCREgrammar.PCREgrammarParser.LiteralContext;
import PCREgrammar.PCREgrammarParser.QuantifierContext;
import PCREgrammar.PCREgrammarParser.Shared_atomContext;
import PCREgrammar.PCREgrammarParser.Shared_literalContext;

public class RegexListener extends PCREgrammarBaseListener {

    private Stack<EpsilonNFA> stack = new Stack<>();

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
        TerminalNode dot = ctx.Dot();
        if (literal != null)
            proccessLiteral(literal);
        else if (character_class != null)
            processCharacter_class(character_class);
        else if (dot != null)
            this.stack.push(new EpsilonNFA(WildcardTransition.class));
    }

    private boolean isEscapedChar(Shared_literalContext ctx)
    {
        String[] escape_chars = {"\\a", "\\e", "\\f", "\\n", "\\r", "\\t"};
        return Arrays.asList(escape_chars).contains(ctx.getText());
    }

    private int[] getShared_literalCodePoints(Shared_literalContext ctx)
    {
        String text = ctx.getText();
        if (text.length() == 1)
            return new int[] {text.charAt(0)};
        
        TerminalNode octal = ctx.OctalChar();
        TerminalNode hex = ctx.HexChar();
        TerminalNode quoted = ctx.Quoted();
        TerminalNode block_quoted = ctx.BlockQuoted();

        if (isEscapedChar(ctx))
            return new int[] {getEscapedCharCodePoint(ctx.getText())};
        if (octal != null)
            return new int[] {getOctalCharCodePoint(octal.getText())};
        if (hex != null)
            return new int[] {getHexCharCodePoint(hex.getText())};
        if (quoted != null)
            return new int[] {getQuotedCodePoint(quoted.getText())};
       
        return getBlockQuotedCodePoints(block_quoted.getText());
    }

    private void proccessLiteral(LiteralContext ctx)
    {
        Shared_literalContext shared_literal = ctx.shared_literal();
        if (shared_literal != null)
        {
            int[] code_points = getShared_literalCodePoints(shared_literal);
            concatCodePoints(code_points);
        }
        else 
            stack.push(new EpsilonNFA(']'));
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

    private int[] getBlockQuotedCodePoints(String block_quoted_str)
    {
        String unescaped_literal = block_quoted_str.substring(2, block_quoted_str.length() - 2);
        return unescaped_literal.codePoints().toArray();
    }

    private void concatCodePoints(int[] code_points)
    {
        for (int i = 0; i < code_points.length; i++) 
        {
            stack.push(new EpsilonNFA(code_points[i]));
            if (i != 0) 
                concat();
        }
    }

    private void alternateCodePoints(int[] code_points)
    {
        for (int i = 0; i < code_points.length; i++) 
        {
            stack.push(new EpsilonNFA(code_points[i]));
            if (i != 0) 
                alternate();
        }
    }

    
    private void alternateCodePointRangeInclusive(int start, int end) 
    {
        try {
            if (start > end)
            throw new Exception("Invalid code point range");
        } catch (Exception e) { 
            e.printStackTrace();
            System.exit(-1);
        }
        
        for (int i = start; i <= end; i++)
        {
            stack.push(new EpsilonNFA(i));
            if (i != start)
            alternate();
        }
    }
    
    private void alternateCodePointRangeExclusive(int start, int end)
    {
        this.alternateCodePointRangeInclusive(start + 1, end - 1);
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
        
        if (ctx.getText().charAt(1) != '^') //non-negated
        {
            List<Cc_atomContext> cc_atoms = ctx.cc_atom();
            for (int i = 0; i < cc_atoms.size(); i++)
            {
                List<Cc_literalContext> cc_literals = cc_atoms.get(i).cc_literal();
                Shared_atomContext shared_atom = cc_atoms.get(i).shared_atom();
                if (cc_literals.size() == 1) // no hyphen
                {
                    int[] code_points = getCc_literalCodePoints(cc_literals.get(0));
                    alternateCodePoints(code_points);
                }
                else if (cc_literals.size() == 2) // hyphen 
                {
                    int[] first_code_points = getCc_literalCodePoints(cc_literals.get(0));
                    int[] second_code_points = getCc_literalCodePoints(cc_literals.get(1));
                    alternateCodePoints(first_code_points);
                    alternateCodePoints(second_code_points);
                    alternate();
                    alternateCodePointRangeExclusive(first_code_points[first_code_points.length - 1], second_code_points[0]);
                    alternate();
                }
                else if (shared_atom != null)
                    processSharedAtom(shared_atom);

                if (i != 0)
                    alternate();
            }
        }
        else // negated
        {

        }
    }

    private void alpha()
    {
        alternateCodePointRangeInclusive('a', 'z');
        alternateCodePointRangeInclusive('A', 'Z');
        alternate();
    }

    private void digit()
    {
        alternateCodePointRangeInclusive('0', '9');
    }

    private void horizontalWhiteSpace()
    {
        alternateCodePoints(new int[]{'\t', ' '});
    }

    private void verticalWhiteSpace()
    {
        alternateCodePoints(new int[]{'\r', '\n', 0x000C, 0x000B, 0x0085, 0x2028, 0x2029});
    }

    private void whiteSpace()
    {
        horizontalWhiteSpace();
        verticalWhiteSpace();
        alternate();
    }

    private void word()
    {
        alpha();
        digit();
        alternate();
        stack.push(new EpsilonNFA('_'));
        alternate();
    }

    private void newLineSequence()
    {
        stack.push(new EpsilonNFA('\r'));
        stack.push(new EpsilonNFA('\n'));
        concat();
        alternateCodePoints(new int[]{'\r', '\n', '\f', 0x000B, 0x0085});
        alternate();
    }

    private void processSharedAtom(Shared_atomContext ctx)
    {
        if (ctx.POSIXNamedSet() != null)
            processPosixSets(ctx.POSIXNamedSet());
        else if (ctx.ControlChar() != null)
            processControlChar(ctx.ControlChar());
        else if (ctx.DecimalDigit() != null)
            digit();
        else if (ctx.HorizontalWhiteSpace() != null)
            horizontalWhiteSpace();
        else if (ctx.NewLineSequence() != null)
            newLineSequence();
        else if (ctx.WhiteSpace() != null)
            whiteSpace();
        else if (ctx.VerticalWhiteSpace() != null)
            verticalWhiteSpace();
        else if (ctx.WordChar() != null)
            word();
    }

    private void processControlChar(TerminalNode control_node) // https://www.pcre.org/original/doc/html/pcrepattern.html#SEC5 \cx
    {
        char character = Character.toUpperCase(control_node.getText().charAt(2));
        if (character >= 0 && character <= 127) //ascii
        {
            BigInteger code = new BigInteger(Integer.toString(character));
            BigInteger flipped_code = code.flipBit(6);
            stack.push(new EpsilonNFA(flipped_code.intValueExact()));
        }
    }

    private void processPosixSets(TerminalNode posix_node)
    {
        String text = posix_node.getText();
        String set_name = text.substring(text.indexOf(':') + 1, text.lastIndexOf(':'));

        switch (set_name)
        {
            case "alnum": // alphanumeric
                alpha();
                digit();
                alternate();
                break;
            case "alpha": //alphabetic
                alpha();
                break;
            case "ascii":       // 0-127
                alternateCodePointRangeInclusive(0, 127);
                break;
            case "blank":       // space or tab
                horizontalWhiteSpace();
                break;
            case "cntrl":       // control character
                alternateCodePointRangeInclusive(0x0000, 0x001F);
                stack.push(new EpsilonNFA(0x007F));
                alternate();
                break;
            case "digit":       // decimal digit
                digit();
                break;
            case "graph":       // printing, excluding space
                alternateCodePointRangeInclusive(0x0021, 0x007E);
                break;
            case "lower":       // lower case letter
                alternateCodePointRangeInclusive('a', 'z');
                break;
            case "print":       // printing, including space
                alternateCodePointRangeInclusive(0x0020, 0x007E);
                break;
            case "punct":       // printing, excluding alphanumeric [!"#$%&'()*+,\-./:;<=>?@[]^_`{|}~]
                alternateCodePoints(new int[]{'!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '\\', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '[', ']', '^', '_', '`', '{', '|', '}', '~'});
                break;
            case "space":       // white space
                whiteSpace();
                break;
            case "upper":       // upper case letter
                alternateCodePointRangeInclusive('A', 'Z');
                break;
            case "word":        // same as \w
                word();
                break;
            case "xdigit":      // hexadecimal digit
                digit();
                alternateCodePointRangeInclusive('a', 'f');
                alternate();
                alternateCodePointRangeInclusive('A', 'F');
                alternate();
                break;
            default:
                try {
                    throw new Exception("Invalid posix named set");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
        }

    }

    private int[] getCc_literalCodePoints(Cc_literalContext ctx)
    {
        Shared_literalContext shared_literal = ctx.shared_literal();
        if (shared_literal != null)
            return getShared_literalCodePoints(shared_literal);
        if (ctx.WordBoundary() != null)
            return new int[] {"\u0008".codePointAt(0)};
        
        return new int[] {ctx.getText().codePointAt(0)};
    }

    public void exitExpr(ExprContext ctx)
    {
        System.out.println("Exited Expr: " + ctx.getText());
        for(int i = 0; i < ctx.element().size() - 1; i++)
            concat();
    }

    public void exitAlternation(AlternationContext ctx)
    {
        System.out.println("Exited Alternation: " + ctx.getText());
        for(int i = 0; i < ctx.Pipe().size(); i++)
            alternate();
    }

    public void enterQuantifier(QuantifierContext ctx)
    {
        EpsilonNFA top = stack.pop();

        if (ctx.Plus() != null) // +
            stack.push(EpsilonNFA.oneOrMore(top)); 
        else if (ctx.QuestionMark() != null) // ?
            stack.push(EpsilonNFA.zeroOrOne(top));
        else if (ctx.Star() != null) // *
            stack.push(EpsilonNFA.zeroOrMore(top));
    }

    public EpsilonNFA getEpsilonNFA()
    {
        return this.stack.peek();
    }
    
}