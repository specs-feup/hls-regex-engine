#include <stdio.h>
#include <string.h>
#include <array>
#include <ctype.h>

class State {
    private:
    bool active;
    int stream_index;

    public:
    State() {
        this->active = false;
        this->stream_index = 0;
    }

    bool isActive() { return this->active; }
    void setActive(bool activation_state) { this->active = activation_state; }
    int getStreamIndex() { return this->stream_index; }
    void setStreamIndex(int stream_index) { this->stream_index = stream_index; }
};

class Fifo {
    private:
    std::array<int, 32> data;
    std::array<int, 32>::iterator head;
    size_t size;

    private:
    void clear() {
        this->data = {};
        this->size = 0;
    }

    public:
    Fifo() { 
        this->clear();
        this->head = this->data.begin();
    }

    void enqueue(int element, bool clear) {
        if (clear)
            this->clear();
        this->data[this->size++] = element;
    }

    void enqueueFifo(Fifo &fifo, bool clear) {
        for (size_t i = 0; i < fifo.size; i++)
            this->enqueue(*(fifo.head + i), clear && i == 0);
    }

    bool checkQueue(char *stream, int stream_size, int stream_index, bool insensitive, int *transition_increment) {
        if ((stream_index == stream_size && this->size != 0) || stream_index > stream_size)
            return false;

        bool valid = true;
        for (size_t i = 0; i < this->size && valid; i++)
        {
            char stream_char = insensitive ? toupper(stream[stream_index++]) : stream[stream_index++];
            char fifo_char = insensitive ? toupper(*(this->head + i)) : *(this->head + i);
            valid = stream_char == fifo_char;
        }

        if (valid)
            *transition_increment = this->size;

        return valid;
    }
};

enum CounterOperation { SET, COMPARE_EQUAL, COMPARE_LESS, COMPARE_EQUALMORE, COMPARE_RANGE };

class Counter {
    private:
    unsigned int value = 0;
    bool increment_lock = false;
    
    public:
    bool performOperation(CounterOperation operation, unsigned int val1, unsigned int val2) {
        bool valid = false;
        switch (operation) {
            case SET:
                this->value = 1;
                return true;
            case COMPARE_EQUAL:
                valid = this->value + !this->increment_lock == val1;
                break;
            case COMPARE_LESS:
                valid = this->value + !this->increment_lock < val1;
                break;
            case COMPARE_EQUALMORE:
                valid = this->value + !this->increment_lock >= val1;
                break;
            case COMPARE_RANGE:
                valid = this->value + !this->increment_lock >= val1 && this->value <= val2;
                break;
        }

        if (valid && !this->increment_lock)
        {
            this->value++;
            this->increment_lock = true;
        }

        return valid;
    }

    void unlock() {
        this->increment_lock = false;
    }
};

enum TransitionType { Character, CharacterClass, Wildcard, Backreference};

template <size_t N>
class Transition {
    protected:
    TransitionType type;
    std::array<int, N> tokens;
    State* current;
    State* next;
    int stream_increment;

    /* Additional Info */
    bool at_start;
    bool at_end;

    /* Type Specific */
    bool negated = false;

    Transition(TransitionType type, const std::array<int, N> tokens, State *current, State *next, int stream_increment, bool at_start, bool at_end) {
        this->type = type;
        this->tokens = tokens;
        this->current = current;
        this->next = next;
        this->stream_increment = stream_increment;
        this->at_start = at_start;
        this->at_end = at_end;
    }

    private:
    bool testStartAnchor(int index, char *stream, bool multiline)
    {
        if (multiline)
            return index == 0 || stream[index - 1] == '\n';
        else
            return index == 0;
    }

    bool testEndAnchor(int index, char *stream, int stream_size, bool multiline)
    {
        if (multiline)
            return index == stream_size || stream[index] == '\n';
        else
            return index == stream_size;
    }

    bool testTokens(std::array<int, N> tokens, char *stream, bool insensitive)
    {
        int stream_token = insensitive ? toupper(stream[this->current->getStreamIndex()]) : stream[this->current->getStreamIndex()];
        bool has_match = false;

        for (int token : tokens)
        {
            if (insensitive)
                token = toupper(token);

            if (token == stream_token)
            {
                has_match = true;
                break;
            }
        }

        return (this->negated && !has_match) || (!this->negated && has_match);
    }

    bool testToken(int token, char *stream, bool insensitive)
    {
        int stream_token = insensitive ? toupper(stream[this->current->getStreamIndex()]) : stream[this->current->getStreamIndex()];
        if (insensitive)
            token = toupper(token);

        return token == stream_token;
    }

    public:
    State *getCurrent() {
        return this->current;
    }

    State *getNext() {
        return this->next;
    }

    void execute()
    {
        this->next->setStreamIndex(this->current->getStreamIndex() + this->stream_increment);        
        this->next->setActive(true);
    }

    bool test(char *stream, int stream_size, bool multiline, bool insensitive, bool singleline)
    {
        if (!this->current->isActive() || this->current->getStreamIndex() >= stream_size)
            return false;

        if (this->at_start && !this->testStartAnchor(this->current->getStreamIndex(), stream, multiline))
            return false;

        switch (this->type)
        {
            case TransitionType::Character:
                if (!this->testToken(this->tokens[0], stream, insensitive))
                    return false;
                break;
            case TransitionType::CharacterClass:
                if (!this->testTokens(this->tokens, stream, insensitive))
                    return false;
                break;
            case TransitionType::Wildcard:
                if (!singleline && stream[this->current->getStreamIndex()] == '\n')
                    return false;
                break;
            default:
                return false;
        }

        if (this->at_end && !this->testEndAnchor(this->current->getStreamIndex() + this->stream_increment, stream, stream_size, multiline))
            return false;

        return true;
    }

    bool testFifo(char *stream, int stream_size, Fifo &fifo, bool multiline, bool insensitive)
    {
        if (this->type != TransitionType::Backreference)
            return false;

        if (!this->current->isActive() || this->current->getStreamIndex() > stream_size)
            return false;

        if (this->at_start && !this->testStartAnchor(this->current->getStreamIndex(), stream, multiline))
            return false;

        if (!fifo.checkQueue(stream, stream_size, this->current->getStreamIndex(), insensitive, &this->stream_increment))
            return false;
        
        if (this->at_end && !this->testEndAnchor(this->current->getStreamIndex() + this->stream_increment, stream, stream_size, multiline))
            return false;

        return true;
    }
};

class CharacterTransition : public Transition<1> {
    public:
    CharacterTransition(int token, State *current, State *next, bool at_start, bool at_end) : Transition<1>(TransitionType::Character, std::array<int, 1>({token}), current, next, 1, at_start, at_end)
    { }
};

template <size_t N>
class CharacterClassTransition : public Transition<N> {
    public:
    CharacterClassTransition(const std::array<int, N> tokens, bool negated, State *current, State *next, bool at_start, bool at_end) : Transition<N>(TransitionType::CharacterClass, tokens, current, next, 1, at_start, at_end)
    { 
        this->negated = negated;
    }
};

class WildcardTransition : public Transition<0> {
    public:
    WildcardTransition(State *current, State *next, bool at_start, bool at_end) : Transition<0>(TransitionType::Wildcard, std::array<int, 0>(), current, next, 1, at_start, at_end)
    { }
};

class BackreferenceTransition : public Transition<0> {
    public:
    BackreferenceTransition(State *current, State *next, bool at_start, bool at_end) : Transition<0>(TransitionType::Backreference, std::array<int, 0>(), current, next, 0, at_start, at_end)
    { }
};

<#list automata as automaton>
//kernel for /${automaton.expression}/
int kernel${automaton?counter}(char stream[], int stream_size)
{
    State current[${automaton.states?size}];
    State next[${automaton.states?size}];

    ${declareFifos(automaton.fifo_ids)}
    ${declareCounters(automaton.counter_ids)}

    <#assign transition_counter = 0>
    <#list automaton.transitions as transition>
      <#if transition.type == "Character">
    CharacterTransition t${transition_counter}(${transition.token}, &current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      <#elseif transition.type == "CharacterClass">
    CharacterClassTransition<${transition.tokens?size}> t${transition_counter}(std::array<int, ${transition.tokens?size}>({${transition.tokens?join(", ")}}), ${transition.negated?c}, &current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      <#elseif transition.type == "Wildcard">
    WildcardTransition t${transition_counter}(&current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      <#elseif transition.type == "Backreference">
    BackreferenceTransition t${transition_counter}(&current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      </#if>
    <#assign transition_counter += 1>
    </#list>

    int transitions_occurred;
    bool in_end_state = false;

    for (int stream_offset = 0; stream_offset < stream_size; stream_offset++) 
    {
        memset(current, 0, ${automaton.states?size} * sizeof(State));
        current[${automaton.start_state.id}].setActive(true);
        current[${automaton.start_state.id}].setStreamIndex(stream_offset);

        do {
            transitions_occurred = 0;
     
        <#assign transition_counter = 0>
        <#list automaton.transitions as transition>
        <#assign callTest = "">
        <#if transition.type == "Backreference">
          <#assign callTest = "t${transition_counter}.testFifo(stream, stream_size, ${transition.fifo_to_match}, ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c})">
        <#else>
          <#assign callTest = "t${transition_counter}.test(stream, stream_size, ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c}, ${hasSinglelineFlag(automaton)?c})">
        </#if>
            if (${callTest}<#list transition.counters_info as counter_info> && ${counter_info.counter.id}.performOperation(CounterOperation::${counter_info.operation}, ${counter_info.counter.target_value1}, ${counter_info.counter.target_value2})</#list>) 
            { 
                t${transition_counter}.execute();
                transitions_occurred++;
                <#list transition.fifos_info as fifo_info>
                <#if transition.type == "Backreference">
                ${fifo_info.fifo.id}.enqueueFifo(${transition.fifo_to_match}, ${fifo_info.clear?c});
                <#else>
                ${fifo_info.fifo.id}.enqueue(stream[t${transition_counter}.getCurrent()->getStreamIndex()], ${fifo_info.clear?c});
                </#if>
                </#list>
            }
        <#assign transition_counter += 1>
        </#list>
            <#list automaton.counter_ids as counter_id>${counter_id}.unlock();</#list>
            if (transitions_occurred)
            {
                memcpy(current, next, ${automaton.states?size} * sizeof(State));
                memset(next, 0, ${automaton.states?size} * sizeof(State));
                in_end_state = <#list automaton.end_states as end_state>current[${end_state.id}].isActive()<#if !end_state?is_last> || </#if></#list>;
            }
      
        } while (transitions_occurred && !in_end_state);

        if (in_end_state)
            return true;
    }

    return false;
}

</#list>
int main()
{
    char stream[256];
    while (1)
    {
        printf("Insert a string: ");
        fgets(stream, 256, stdin);
        if ((strlen(stream) > 0) && (stream[strlen (stream) - 1] == '\n'))
            stream[strlen (stream) - 1] = '\0';
        int match = <#list automata as automaton>kernel${automaton?counter}(stream, strlen(stream))<#if !automaton?is_last> || </#if></#list>;
        match ? printf("\n\033[0;32mIt's a match! :)\n\n\033[0m") : printf("\n\033[0;31mIt's not a match :(\n\n\033[0m");
    }

    return 0;
}

<#function declareCounters counter_ids>
  <#assign result = "">
    <#list counter_ids as counter_id>
      <#assign result = result + "Counter ${counter_id};">
       <#if !counter_id?is_last>
        <#assign result = result + "\n\t\t">
       </#if>
    </#list>
  <#return result>
</#function>

<#function declareFifos fifo_ids>
  <#assign result = "">
    <#list fifo_ids as fifo_id>
      <#assign result = result + "Fifo ${fifo_id};">
       <#if !fifo_id?is_last>
        <#assign result = result + "\n\t\t">
       </#if>
    </#list>
  <#return result>
</#function>


<#function hasCaseInsensitiveFlag automaton>
  <#return automaton.flags?contains("i")>
</#function>

<#function hasMultilineFlag automaton>
  <#return automaton.flags?contains("m")>
</#function>

<#function hasSinglelineFlag automaton>
  <#return automaton.flags?contains("s")>
</#function>