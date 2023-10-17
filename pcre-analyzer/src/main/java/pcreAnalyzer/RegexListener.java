// Generated from regexParser.g4 by ANTLR 4.12.0
package pcreAnalyzer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import PCREgrammar.PCREgrammarBaseListener;
import PCREgrammar.PCREgrammarParser.AlternationContext;
import PCREgrammar.PCREgrammarParser.AtomContext;
import PCREgrammar.PCREgrammarParser.BackreferenceContext;
import PCREgrammar.PCREgrammarParser.CalloutContext;
import PCREgrammar.PCREgrammarParser.CaptureContext;
import PCREgrammar.PCREgrammarParser.Character_classContext;
import PCREgrammar.PCREgrammarParser.ConditionalContext;
import PCREgrammar.PCREgrammarParser.ElementContext;
import PCREgrammar.PCREgrammarParser.ExprContext;
import PCREgrammar.PCREgrammarParser.LiteralContext;
import PCREgrammar.PCREgrammarParser.Look_aroundContext;
import PCREgrammar.PCREgrammarParser.Non_captureContext;
import PCREgrammar.PCREgrammarParser.ParseContext;
import PCREgrammar.PCREgrammarParser.QuantifierContext;
import PCREgrammar.PCREgrammarParser.Quantifier_typeContext;
import PCREgrammar.PCREgrammarParser.Shared_literalContext;
import PCREgrammar.PCREgrammarParser.Subroutine_referenceContext;

public class RegexListener extends PCREgrammarBaseListener {

    private class LengthDouble {
        double value;
        boolean fixed;

        LengthDouble(double value) {
            this.value = value;
            this.fixed = true;
        }
    }

    private boolean locked;
    private RulesAnalyzer analyzer;
    private String flags;
    private int fifo_counter;
    private Stack<Integer> fifos;
    private Map<String, Integer> fifo_aliases;

    private double expression_length;
    private Map<Integer, LengthDouble> capture_groups_lengths;
    private Map<Integer, LengthDouble> referenced_capture_group_lengths;
    private Stack<LengthDouble> active_capture_groups_lengths;

    public RegexListener(RulesAnalyzer analyzer, String flags)
    {
        this.fifo_counter = 0;
        this.fifos = new Stack<>();
        this.fifo_aliases = new HashMap<>();
        this.locked = false;
        this.analyzer = analyzer;
        this.flags = flags;
        this.expression_length = 0;
        this.capture_groups_lengths = new LinkedHashMap<>();
        this.referenced_capture_group_lengths = new LinkedHashMap<>();
        this.active_capture_groups_lengths = new Stack<>();
    }

    private boolean hasExtendedFlag()
    {
        return this.flags.indexOf('x') != -1;
    }

    private void addOccurrence(String operation)
    {
        if (!this.locked)
            this.analyzer.addOperatorOccurrence(operation);
    }

    private void unfixActiveGroupLength()
    {
        if (!this.active_capture_groups_lengths.isEmpty())
            this.active_capture_groups_lengths.peek().fixed = false;
    }

    private double getElementLength(ElementContext ctx)
    {
        AtomContext atom_ctx = ctx.atom();
        double length = 0;
        if ((atom_ctx.literal() != null && atom_ctx.literal().shared_literal() != null) || atom_ctx.shared_atom() != null || atom_ctx.character_class() != null)
            length = 1;
        
        if (atom_ctx.backreference() != null)
        {
            if (atom_ctx.backreference().backreference_or_octal() != null && (atom_ctx.backreference().backreference_or_octal().OctalChar() != null || (atom_ctx.backreference().backreference_or_octal().digit() != null && atom_ctx.backreference().backreference_or_octal().digit().getText().equals("0"))))
                length = 1;
            else 
            {
                int backreference_index = getBackreferenceIndex(atom_ctx.backreference());
                length = this.capture_groups_lengths.get(backreference_index).value;
            }
        }

        if (ctx.quantifier() == null)
            return length;
        else
        {
            if (ctx.quantifier().QuestionMark() != null)
                return length;
            if (ctx.quantifier().Plus() != null || ctx.quantifier().Star() != null)
                return Double.POSITIVE_INFINITY;
         
            if (ctx.quantifier().number().size() == 1)
            {
                if (ctx.quantifier().Comma() != null)
                    return Double.POSITIVE_INFINITY;
                else 
                    return length * Double.parseDouble(ctx.quantifier().number(0).getText());
            }
            else 
                return length * Double.parseDouble(ctx.quantifier().number(1).getText());
        }
    }

    public void enterLiteral(LiteralContext ctx)
    {
        Shared_literalContext shared_literal = ctx.shared_literal();
        if (shared_literal != null && shared_literal.Hash() != null && this.hasExtendedFlag())
            this.locked = true;
    }

    public void enterAtom(AtomContext ctx)
    {
        if (ctx.Caret() != null || ctx.StartOfSubject() != null)
            addOccurrence("Start Anchor");
        else if (ctx.EndOfSubjectOrLine() != null || ctx.EndOfSubjectOrLineEndOfSubject() != null)
            addOccurrence("End Anchor");
    }

    public void exitElement(ElementContext ctx)
    {
        boolean is_first = ((ExprContext)ctx.parent).element(0).equals(ctx);
        if (!is_first)
            addOccurrence("Concatenations");
    }

    public void exitAlternation(AlternationContext ctx)
    {
        double biggest_expr_length = 0;
        for (ExprContext expr_ctx : ctx.expr())
        {
            double expr_length = 0;
            for (ElementContext element_ctx : expr_ctx.element())
                expr_length += getElementLength(element_ctx);

            if (expr_length > biggest_expr_length)
                biggest_expr_length = expr_length;
        }

        this.expression_length += biggest_expr_length;
        if (!active_capture_groups_lengths.isEmpty())
            active_capture_groups_lengths.peek().value += biggest_expr_length;

        for(int i = 0; i < ctx.Pipe().size(); i++)
            addOccurrence("Alternations");

        if (!ctx.Pipe().isEmpty())
            unfixActiveGroupLength();
    }

    public void enterQuantifier(QuantifierContext ctx)
    {
        addOccurrence("Quantifiers");

        if (ctx.OpenBrace() != null)
            addOccurrence("Bounded Quantifiers");

        if (ctx.OpenBrace() == null || ctx.Comma() != null)
            unfixActiveGroupLength();
    }

    public void enterCapture(CaptureContext ctx)
    {
        // addOccurrence("Capture Groups");
        fifos.push(this.fifo_counter);

        if (ctx.name() != null)
            this.fifo_aliases.put(ctx.name().getText(), this.fifo_counter);

        active_capture_groups_lengths.push(new LengthDouble(0.0));
        this.fifo_counter++;
    }

    public void exitCapture(CaptureContext ctx)
    {
        LengthDouble current_group_length = active_capture_groups_lengths.pop();
        this.capture_groups_lengths.put(fifos.pop(), current_group_length);
        if (!active_capture_groups_lengths.isEmpty())
            active_capture_groups_lengths.peek().value += current_group_length.value;
    }

    private int getBackreferenceIndex(BackreferenceContext ctx)
    {
        int backreference_index;
        if (ctx.name() != null)
        {
            String name = ctx.name().getText();
            if (this.fifo_aliases.containsKey(name))
                backreference_index = this.fifo_aliases.get(name);
            else
                throw new RuntimeException("No capture group named " + ctx.name().getText() + " found");
        }
        else if (ctx.number() != null)
            backreference_index = Integer.parseInt(ctx.number().getText()) - 1;
        else
            backreference_index = Integer.parseInt(ctx.backreference_or_octal().digit().getText()) - 1;

        return backreference_index;
    }

    public void enterBackreference(BackreferenceContext ctx)
    {
        if (ctx.backreference_or_octal() != null && (ctx.backreference_or_octal().OctalChar() != null || (ctx.backreference_or_octal().digit() != null && ctx.backreference_or_octal().digit().getText().equals("0"))))
            return;
        else 
        {
            int backreference_index = getBackreferenceIndex(ctx);
            this.referenced_capture_group_lengths.put(backreference_index, this.capture_groups_lengths.get(backreference_index));
            addOccurrence("Backreferences");
            unfixActiveGroupLength();
        }
    }

    public void exitParse(ParseContext ctx)
    {
        this.analyzer.addExpressionLength(this.expression_length);

        List<Double> group_lengths = new LinkedList<>();
        for (LengthDouble length : this.capture_groups_lengths.values())
            group_lengths.add(length.value);
        this.analyzer.addCaptureGroupLengths(group_lengths);

        List<Double> referenced_group_lengths = new LinkedList<>();
        List<Double> fixed_referenced_group_lengths = new LinkedList<>();
        for (LengthDouble length : this.referenced_capture_group_lengths.values())
        {
            referenced_group_lengths.add(length.value);
            if (length.fixed)
                fixed_referenced_group_lengths.add(length.value);
        }
        this.analyzer.addReferencedCaptureGroupLengths(referenced_group_lengths);
        this.analyzer.addFixedReferencedCaptureGroupLengths(fixed_referenced_group_lengths);
    }

    public void enterCharacter_class(Character_classContext ctx)
    {
        addOccurrence("Character Classes");
    }

    public void enterQuantifier_type(Quantifier_typeContext ctx)
    {
        if (ctx.Plus() != null)
            addOccurrence("Possessive Quantifiers");
        else if (ctx.QuestionMark() != null)
            addOccurrence("Lazy Quantifiers");
    }

    public void enterNon_capture(Non_captureContext ctx)
    {
        addOccurrence("Non-Capture Groups");
    }

    public void enterLook_around(Look_aroundContext ctx)
    {
        addOccurrence("LookArounds");
    }

    public void enterSubroutine_reference(Subroutine_referenceContext ctx)
    {
        addOccurrence("Subroutines");
    }

    public void enterConditional(ConditionalContext ctx)
    {
        addOccurrence("Conditional Patterns");
    }

    public void enterCallout(CalloutContext ctx)
    {
        addOccurrence("Callouts");
    }
    
}