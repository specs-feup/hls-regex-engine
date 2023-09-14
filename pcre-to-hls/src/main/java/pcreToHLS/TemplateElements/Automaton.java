package pcreToHLS.TemplateElements;

import java.util.Set;

public class Automaton {
    private String expression;
    private Set<String> counter_ids;
    private Set<State> states;
    private State start_state;
    private Set<State> end_states;

    public Automaton() {}

    public Automaton(String expression, Set<String> counter_ids, Set<State> states, State start_state, Set<State> end_states)
    {
        this.expression = expression;
        this.counter_ids = counter_ids;
        this.states = states;
        this.start_state = start_state;
        this.end_states = end_states;
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

    public void setCounter_ids(Set<String> counter_ids) {
        this.counter_ids = counter_ids;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
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

}
