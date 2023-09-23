package pcreToHLS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import PCREgrammar.PCREgrammarLexer;
import PCREgrammar.PCREgrammarParser;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import pcreToHLS.TemplateElements.Automaton;
import pcreToHLS.TemplateElements.State;
import pcreToHLS.TemplateElements.TransitionGroup;

public class CodeGenerator {

    private class PCRE {
        public String expression;
        public String flags;
        public PCRE(Entry<String, String> pcre_data) {
            this.expression = pcre_data.getKey();
            this.flags = pcre_data.getValue();
        }
        public String toString(){
            return "/" + this.expression + "/" + this.flags;
        }
    };

    private Map<PCRE, FinalAutomaton> regexes;
    private RulesAnalyzer analyzer;

    public CodeGenerator(Map<String, String> expressions, boolean dfas)
    {
        this.analyzer = new RulesAnalyzer();
        this.regexes = new HashMap<>();
        for (Entry<String, String> expression_entry : expressions.entrySet())
        {
            PCRE regex = new PCRE(expression_entry);
            CharStream stream = CharStreams.fromString(regex.expression);
            PCREgrammarLexer lexer = new PCREgrammarLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PCREgrammarParser parser = new PCREgrammarParser(tokens);
            ParseTree tree = parser.parse();

            this.analyzer.addFlagsOccurrence(regex.flags);

            // System.out.println("\n=== Parse Tree ===");
            // System.out.println(TreeUtils.toPrettyTree(tree, parser));
            try {
                FinalAutomaton automaton;
                if (dfas)
                    automaton = new DFA(tree, this.analyzer, regex.flags);
                else 
                    automaton = new NFA(tree, this.analyzer, regex.flags);
                this.regexes.put(regex, automaton);
            }
            catch (EmptyStackException e) { System.out.println("Failed to parse: " + regex); }
            
        }
    }

    public RulesAnalyzer getAnalyzer() {
        return analyzer;
    }

    private State getState(String vertex, Map<String, State> vertices_mapping)
    {
        State state;
        if (vertices_mapping.containsKey(vertex))
            state = vertices_mapping.get(vertex);
        else 
        {
            state = new State();
            state.setId(vertices_mapping.size());
            vertices_mapping.put(vertex, state);
        }

        return state;
    }

    public void generate(String path)
    {
        Set<Automaton> automata = new HashSet<>();

        for (Entry<PCRE, FinalAutomaton> regex_entry : this.regexes.entrySet())
        {
            FinalAutomaton automaton = regex_entry.getValue();
            PCRE regex = regex_entry.getKey();
            Graph<String, DefaultEdge> automaton_graph = automaton.getGraph();
            Map<String, State> vertex_ids = new HashMap<>();
            Set<State> end_states = new HashSet<>();
            Set<String> counter_ids = new HashSet<>();

            for (String vertex : automaton_graph.vertexSet())
            {
                State curr_state = getState(vertex, vertex_ids);
                Set<DefaultEdge> outgoing = automaton_graph.outgoingEdgesOf(vertex);
                for (DefaultEdge edge : outgoing) 
                {
                    String target = automaton_graph.getEdgeTarget(edge);
                    State target_state = getState(target, vertex_ids);
                    try {
                        TransitionGroup edge_transitions = ((LabeledEdge<?>) edge).generateTransitions(target_state);
                        curr_state.addTransitionGroup(edge_transitions);
                        List<CounterInfo> group_counter_infos = edge_transitions.getCounter_infos();
                        for (CounterInfo info : group_counter_infos)
                            counter_ids.add(info.counter.getId());
                    } catch (UnsupportedOperationException e) {
                        System.out.println("error because of bounded quantifier (to remove)"); //TODO REMOVE THIS AFTER FIX
                        continue;
                    }

                }

                if (automaton.getEnds().contains(vertex))
                    end_states.add(curr_state);
            }

            Set<State> states = new HashSet<>(vertex_ids.values());
            State start_state = vertex_ids.get(automaton.getStart());

            automata.add(new Automaton(regex.expression, regex.flags, counter_ids, states, start_state, end_states));
        }
        
        Map<String, Object> root = new HashMap<>();
        root.put("automata", automata);
  
        try {
            Template template = FreeMarkerConfig.INSTANCE.getConfig().getTemplate(FreeMarkerConfig.INSTANCE.getTemplateName());
            FileOutputStream file = new FileOutputStream(path);
            Writer out = new OutputStreamWriter(file);
            template.process(root, out);
        } catch (IOException | TemplateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
