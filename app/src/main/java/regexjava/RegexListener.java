// Generated from regexParser.g4 by ANTLR 4.12.0
package regexjava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import javax.annotation.Signed;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import PCREgrammar.PCREgrammarBaseListener;
import PCREgrammar.PCREgrammarParser.AlternationContext;
import PCREgrammar.PCREgrammarParser.AtomContext;
import PCREgrammar.PCREgrammarParser.Character_classContext;
import PCREgrammar.PCREgrammarParser.ExprContext;
import PCREgrammar.PCREgrammarParser.LiteralContext;
import PCREgrammar.PCREgrammarParser.QuantifierContext;
import PCREgrammar.PCREgrammarParser.Shared_literalContext;

public class RegexListener extends PCREgrammarBaseListener {

    private Stack<EpsilonNFA> stack = new Stack<>();

    public void enterAtom(AtomContext ctx)
    {
        LiteralContext literal = ctx.literal();
        if (literal != null)
            proccessLiteral(literal);
    }

    private boolean isEscapedChar(Shared_literalContext ctx)
    {
        String[] escape_chars = {"\\a", "\\e", "\\f", "\\n", "\\r", "\\t"};
        return Arrays.asList(escape_chars).contains(ctx.getText());
    }

    private void proccessLiteral(LiteralContext ctx)
    {
        String text = ctx.getText();
        System.out.println("Entered literal: " + text);
        if (text.length() == 1)
            stack.push(new EpsilonNFA(text.charAt(0)));
        else
        {
            Shared_literalContext shared_literal = ctx.shared_literal();
            TerminalNode octal = shared_literal.OctalChar();
            TerminalNode hex = shared_literal.HexChar();
            TerminalNode quoted = shared_literal.Quoted();
            TerminalNode block_quoted = shared_literal.BlockQuoted();
            if (isEscapedChar(shared_literal))
                stack.push(new EpsilonNFA(getEscapedCharCodePoint(shared_literal.getText())));
            else if (octal != null)
                stack.push(new EpsilonNFA(getOctalCharCodePoint(octal.getText())));
            else if (hex != null)
                stack.push(new EpsilonNFA(getHexCharCodePoint(hex.getText())));
            else if (quoted != null)
                stack.push(new EpsilonNFA(getQuotedCodePoint(quoted.getText())));
            else
            {
                int[] quoted_code_points = getBlockQuotedCodePoints(block_quoted.getText());
                for (int i = 0; i < quoted_code_points.length; i++)
                {
                    stack.push(new EpsilonNFA(quoted_code_points[i]));
                    if (i != 0)
                    {
                        EpsilonNFA second = stack.pop();
                        EpsilonNFA first = stack.pop();
                        stack.push(EpsilonNFA.concat(first, second));
                    }
                }
            }
        }
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
                unescaped_char = "\u000C";
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
        int code_point;

        if (unescaped_hex.length() == 0)
            code_point = 0;
        else 
            code_point = Integer.parseInt(unescaped_hex, 16);

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

    public void enterCharacter_class(Character_classContext ctx)
    {
        System.out.println("Entered char class: " + ctx.getText());
    }

    public void exitExpr(ExprContext ctx)
    {
        System.out.println("Exited Expr: " + ctx.getText());
        for(int i = 0; i < ctx.element().size() - 1; i++)
        {
            EpsilonNFA second = stack.pop();
            EpsilonNFA first = stack.pop();
            stack.push(EpsilonNFA.concat(first, second));
        }
    }

    public void exitAlternation(AlternationContext ctx)
    {
        System.out.println("Exited Alternation: " + ctx.getText());
        for(int i = 0; i < ctx.Pipe().size(); i++)
        {
            EpsilonNFA second = stack.pop();
            EpsilonNFA first = stack.pop();
            stack.push(EpsilonNFA.join(first, second));
        }
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