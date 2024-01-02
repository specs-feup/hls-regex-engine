package pcreToHLS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Comparator;
import java.util.Arrays;

import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;
import org.jgrapht.traverse.GraphIterator;

import PCREgrammar.PCREgrammarLexer;
import PCREgrammar.PCREgrammarParser;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import pcreToHLS.TemplateElements.Automaton;
import pcreToHLS.TemplateElements.BackreferenceTransition;
import pcreToHLS.TemplateElements.State;
import pcreToHLS.TemplateElements.Transition;

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

    public CodeGenerator(Map<String, String> expressions, boolean debug, boolean dfas, boolean remove_unused, boolean expand_fixed, boolean expand_quantifiers)
    {
        this.regexes = new HashMap<>();
        for (Entry<String, String> expression_entry : expressions.entrySet())
        {
            PCRE regex = new PCRE(expression_entry);
            CharStream stream = CharStreams.fromString(regex.expression);
            PCREgrammarLexer lexer = new PCREgrammarLexer(stream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PCREgrammarParser parser = new PCREgrammarParser(tokens);
            ParseTree tree = parser.parse();


            if (debug)
            {
                System.out.println("\n=== Parse Tree ===");
                System.out.println(TreeUtils.toPrettyTree(tree, parser));
                TreeViewer viewr = new TreeViewer(Arrays.asList(parser.getRuleNames()),tree);
                viewr.open();
            }


            try {
                FinalAutomaton automaton;
                if (dfas)
                    automaton = new DFA(tree, regex.flags, debug, remove_unused, expand_fixed, expand_quantifiers);
                else 
                    automaton = new NFA(tree, regex.flags, debug, remove_unused, expand_fixed, expand_quantifiers);
                this.regexes.put(regex, automaton);
            }
            catch (Exception e) {
                System.out.println("Failed to parse: " + regex + ". Error: " + e.getMessage());
                // e.printStackTrace();
            }
            
        }
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
        Comparator<State> state_comparator = new Comparator<State>() {
            @Override
            public int compare(State state1, State state2) {
                boolean hasBackreference1 = state1.getOutgoing_transitions().stream()
                        .anyMatch(transition -> transition instanceof BackreferenceTransition);
                boolean hasBackreference2 = state2.getOutgoing_transitions().stream()
                        .anyMatch(transition -> transition instanceof BackreferenceTransition);

                if (hasBackreference1 && !hasBackreference2) {
                    return -1; // state1 comes first
                } else if (!hasBackreference1 && hasBackreference2) {
                    return 1; // state2 comes first
                } else {
                    return 0; // maintain the original order if both have or don't have Backreference
                }
            }
        };

        for (Entry<PCRE, FinalAutomaton> regex_entry : this.regexes.entrySet())
        {
            FinalAutomaton automaton = regex_entry.getValue();
            PCRE regex = regex_entry.getKey();
            Graph<String, DefaultEdge> automaton_graph = automaton.getGraph();
            Map<String, State> vertex_ids = new LinkedHashMap<>();
            Set<State> end_states = new HashSet<>();
            Set<String> counter_ids = new HashSet<>();
            Set<String> fifo_ids = new HashSet<>();
            List<Transition> transitions = new LinkedList<>();

            GraphIterator<String, DefaultEdge> iterator = new DepthFirstIterator<String, DefaultEdge>(automaton_graph, automaton.start);
            while (iterator.hasNext())
            {
                String vertex = iterator.next();
                for (DefaultEdge edge : automaton_graph.outgoingEdgesOf(vertex))
                {
                    State source_state = getState(vertex, vertex_ids);
                    State target_state = getState(automaton_graph.getEdgeTarget(edge), vertex_ids);
                    Transition edge_transition;
                    try {
                        edge_transition = ((LabeledEdge<?>) edge).generateTransition(source_state, target_state);
                        transitions.add(edge_transition);
                        vertex_ids.get(vertex).addTransition(edge_transition);

                        for (FifoInfo fifo_info : edge_transition.getFifos_info())
                            fifo_ids.add(fifo_info.getFifo().getId());
                        for (CounterInfo counter_info : edge_transition.getCounters_info())
                            counter_ids.add(counter_info.getCounter().getId());
                            
                    } catch (UnsupportedOperationException e) {
                        e.printStackTrace();
                    }
                }
            }

            Collections.sort(transitions, (transition1, transition2) -> {
                if (transition1 instanceof BackreferenceTransition
                        && !(transition2 instanceof BackreferenceTransition)) {
                    return -1;
                } else if (!(transition1 instanceof BackreferenceTransition)
                        && transition2 instanceof BackreferenceTransition) {
                    return 1;
                } else {
                    return 0;
                }
            });

            for (String end_vertex : automaton.getEnds())
                end_states.add(getState(end_vertex, vertex_ids));

            List<State> states = new ArrayList<>(vertex_ids.values());
            Collections.sort(states, state_comparator);
            State start_state = vertex_ids.get(automaton.getStart());

            System.out.println("\n== DATA FOR " + regex.expression + " ==");
            System.out.println("State no: " + states.size());
            System.out.println("Transition no: " + transitions.size() + "\n");

            automata.add(new Automaton(regex.expression, regex.flags, counter_ids, fifo_ids, new LinkedHashSet<>(states), transitions, start_state, end_states));
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
