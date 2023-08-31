// Generated from regexParser.g4 by ANTLR 4.12.0
package regexjava;

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

    private void proccessLiteral(LiteralContext ctx)
    {
        System.out.println("Entered literal: " + ctx.getText());
        Shared_literalContext shared_literal = ctx.shared_literal();
        if ((shared_literal != null && (shared_literal.digit() != null || shared_literal.letter() != null)) || ctx.CharacterClassEnd() != null)
            stack.push(new EpsilonNFA(ctx.getText().charAt(0)));        

        // TODO: DEAL WITH ESCAPED CHARS
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