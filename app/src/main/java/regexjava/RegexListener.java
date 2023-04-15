// Generated from regexParser.g4 by ANTLR 4.12.0
package regexjava;

import java.util.Stack;

import javax.annotation.Signed;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import regexgrammar.regexParserBaseListener;
import regexgrammar.regexParser.AtomContext;
import regexgrammar.regexParser.BranchContext;
import regexgrammar.regexParser.CharClassContext;
import regexgrammar.regexParser.PieceContext;
import regexgrammar.regexParser.QuantifierContext;

public class RegexListener extends regexParserBaseListener {

    private Stack<EpsilonNFA> stack = new Stack<>();

    public void enterAtom(AtomContext ctx)
    {
        TerminalNode single_char = ctx.Char();
        CharClassContext char_class = ctx.charClass();

        if (single_char != null)
            stack.push(new EpsilonNFA(single_char.getText().charAt(0)));
        else if (char_class != null)
        {
            if (char_class.WildcardEsc() != null)
                stack.push(new EpsilonNFA(WildcardTransition.class));
        }
    }

    public void enterQuantifier(QuantifierContext ctx)
    {
        EpsilonNFA top = stack.pop();

        if (ctx.PLUS() != null) // +
            stack.push(EpsilonNFA.oneOrMore(top)); 
        else if (ctx.QUESTION() != null) // ?
            stack.push(EpsilonNFA.zeroOrOne(top));
        else if (ctx.STAR() != null) // *
            stack.push(EpsilonNFA.zeroOrMore(top));
    }

    public void exitBranch(BranchContext ctx)
    {
        int n_pieces = ctx.getChildCount();

        if (n_pieces > 1) // .
        {
            for (int i = 0; i < n_pieces - 1; i++) 
            {
                EpsilonNFA second = stack.pop();
                EpsilonNFA first = stack.pop();
                stack.push(EpsilonNFA.concat(first, second));
            }
        }

        if (isAfterPipe(ctx)) // |
        {
            EpsilonNFA second = stack.pop();
            EpsilonNFA first = stack.pop();
            stack.push(EpsilonNFA.join(first, second));
        }
    }

    private boolean isAfterPipe(BranchContext ctx)
    {
        return !ctx.getParent().getChild(0).equals(ctx);
    }

    public EpsilonNFA getEpsilonNFA()
    {
        return this.stack.peek();
    }
    
}