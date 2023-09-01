package regexjava;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.jgrapht.graph.DefaultEdge;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import regexjava.TemplateElements.State;
import regexjava.TemplateElements.Transition;

public class CodeGenerator {
    private NFA automata;
    private String raw_regex;

    public CodeGenerator(String raw_regex, ParseTree root)
    {
        this.raw_regex = raw_regex;
        this.automata = new NFA(root);
    }

    public NFA getAutomata()
    {
        return this.automata;
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
        Map<String, State> vertex_ids = new HashMap<>();
        Set<State> end_states = new HashSet<>();
        for (String vertex : this.automata.getGraph().vertexSet())
        {
            State curr_state = getState(vertex, vertex_ids);
            Set<DefaultEdge> outgoing = this.automata.getGraph().outgoingEdgesOf(vertex);
            for (DefaultEdge edge : outgoing) 
            {
                String target = this.automata.getGraph().getEdgeTarget(edge);
                State target_state = getState(target, vertex_ids);
                Transition transition = new Transition();
                transition.setTarget(target_state);

                if (edge.getClass() == RegularTransition.class)
                    transition.setToken(((RegularTransition)edge).getCodePoint());
                else
                    transition.setWildcard(true);

                curr_state.addTransition(transition);
            }

            if (this.automata.getEnds().contains(vertex))
                end_states.add(curr_state);
        }
        
        Map<String, Object> root = new HashMap<>();
        Set<State> states = new HashSet<>(vertex_ids.values());
        root.put("raw_regex", this.raw_regex);
        root.put("total_states", states.size());
        root.put("states", states);
        root.put("end_states", end_states);
        root.put("start_state", vertex_ids.get(this.automata.getStart()));

        try {
            Template template = FreeMarkerConfig.INSTANCE.getConfig().getTemplate(FreeMarkerConfig.INSTANCE.getTemplateName());
            FileOutputStream file = new FileOutputStream(path);
            Writer out = new OutputStreamWriter(file);
            template.process(root, out);
        } catch (IOException | TemplateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
