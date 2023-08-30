// Generated from regexParser.g4 by ANTLR 4.12.0
package regexjava;

import java.util.Stack;

import javax.annotation.Signed;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import PCREgrammar.PCREgrammarBaseListener;
import PCREgrammar.PCREgrammarParser.AtomContext;
import PCREgrammar.PCREgrammarParser.Character_classContext;
import PCREgrammar.PCREgrammarParser.QuantifierContext;
import PCREgrammar.PCREgrammarParser.Shared_literalContext;

public class RegexListener extends PCREgrammarBaseListener {

    private Stack<EpsilonNFA> stack = new Stack<>();

    public void enterShared_literal(Shared_literalContext ctx)
    {
        if (ctx.digit() != null || ctx.letter() != null)
            stack.push(new EpsilonNFA(ctx.getText().charAt(0)));
        
        //TODO: DEAL WITH ESCAPED CHARS

            //         if (char_class.WildcardEsc() != null)
    //             stack.push(new EpsilonNFA(WildcardTransition.class));
    //     }
        
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