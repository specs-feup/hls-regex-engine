package pcreToHLS.TemplateElements;

import java.util.List;
import java.util.Set;

public class Automaton {
    private String expression;
    private String flags;
    private Set<String> counter_ids;
    private Set<String> fifo_ids;
    private Set<State> states;
    private List<Transition> transitions;
    private State start_state;
    private Set<State> end_states;
    private boolean has_string_start_anchor;

    public Automaton() {}

    public Automaton(String expression, String flags, Set<String> counter_ids, Set<String> fifo_ids, Set<State> states, List<Transition> transitions, State start_state, Set<State> end_states, boolean has_string_start_anchor)
    {
        this.expression = expression;
        this.flags = flags;
        this.counter_ids = counter_ids;
        this.fifo_ids = fifo_ids;
        this.states = states;
        this.transitions = transitions;
        this.start_state = start_state;
        this.end_states = end_states;
        this.has_string_start_anchor = has_string_start_anchor;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Set<String> getCounter_ids() {
        return counter_ids;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public Set<String> getFifo_ids() {
        return fifo_ids;
    }

    public void setFifo_ids(Set<String> fifo_ids) {
        this.fifo_ids = fifo_ids;
    }

    public void setCounter_ids(Set<String> counter_ids) {
        this.counter_ids = counter_ids;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public State getStart_state() {
        return start_state;
    }

    public void setStart_state(State start_state) {
        this.start_state = start_state;
    }

    public Set<State> getEnd_states() {
        return end_states;
    }

    public void setEnd_states(Set<State> end_states) {
        this.end_states = end_states;
    }

    public boolean isHas_string_start_anchor() {
        return has_string_start_anchor;
    }

    public void setHas_string_start_anchor(boolean has_string_start_anchor) {
        this.has_string_start_anchor = has_string_start_anchor;
    }

}
