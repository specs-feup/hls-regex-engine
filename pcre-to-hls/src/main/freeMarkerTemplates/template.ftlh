#include <stdio.h>
#include <string.h>
#include <array>
#include <ctype.h>

enum CounterOperation { SET, COMPARE_EQUAL, COMPARE_LESS, COMPARE_EQUALMORE, COMPARE_RANGE };

struct CounterCheck {
    const CounterOperation op;
    const unsigned int val1;
    const unsigned int val2;
};

class Counter {
    private:
    unsigned int value = 0;
    unsigned int value_offset = 0;
    
    public:
    bool check(CounterCheck counter_check) const 
    {
        bool valid = false;
        for (unsigned int i = 0; i <= this->value_offset && !valid; i++)
        {
            unsigned int possible_value = this->value - i;

            switch (counter_check.op) {
            case SET:
                valid = true;
                break;
            case COMPARE_EQUAL:
                valid = possible_value + 1 == counter_check.val1;
                break;
            case COMPARE_LESS:
                valid = possible_value + 1 < counter_check.val1;
                break;
            case COMPARE_EQUALMORE:
                valid = possible_value + 1 >= counter_check.val1;
                break;
            case COMPARE_RANGE:
                valid = possible_value + 1 >= counter_check.val1 && possible_value + 1 <= counter_check.val2;
                break;
            }
        }

        return valid;
    }

    void execute(CounterOperation operation, bool clear)
    {
        if (operation == SET && clear)
        {
            this->value = 1;
            this->value_offset = 0;
        }
        else
            this->value++;
    }

    void addOffset() 
    {
        this->value_offset++;
    }
};

class Fifo {
    private:
    std::array<int, 32> data;
    size_t size;
    unsigned int allowed_offsets;

    void clear() 
    {
        this->size = 0;
        this->allowed_offsets = 0;
    }

    public:
    Fifo() : data({}), size(0)
    { }

    void addOffset()
    {
        this->allowed_offsets++;
    }

    void enqueue(int element, bool clear) 
    {
        if (clear)
            this->clear();
        this->data[this->size++] = element;
    }

    void enqueueFifo(const Fifo &fifo, bool clear)
    {
        for (size_t i = 0; i < fifo.size; i++)
            this->enqueue(fifo.data[i], clear && i == 0);
    }

    bool checkQueue(const char *stream, int stream_size, int stream_index, bool insensitive, int *transition_increment) const 
    {
        if ((stream_index == stream_size && this->size != 0) || stream_index > stream_size)
            return false;

        for (int i = 0; i <= this->allowed_offsets; i++)
        {
            bool valid = true;
            for(int j = 0; j + i < this->size && valid; j++)
            {
                char stream_char = stream[stream_index + j];
                char fifo_char = this->data[j + i];
                valid = insensitive ? toupper(stream_char) == toupper(fifo_char) : stream_char == fifo_char;
            }

            if (valid)
            {
                *transition_increment = this->size - i;
                return true;
            }
        }

        return false;
    }
};

template <size_t C, size_t F>
class State {
    private:
    bool active;
    int stream_index;
    std::array<Counter, C> counters;
    std::array<Fifo, F> fifos;

    public:
    State() : active(false), stream_index(0), counters({}), fifos({}) 
    { }

    bool isActive() const
    { 
        return this->active; 
    }

    void setActive(bool activation_state) 
    { 
        this->active = activation_state; 
    }

    int getStreamIndex() const
    {
        return this->stream_index; 
    }

    void setStreamIndex(int stream_index) 
    {
        this->stream_index = stream_index; 
    }

    std::array<Counter, C> getCounters() const
    {
        return this->counters; 
    }

    Counter getCounter(unsigned int index) const
    {
        return this->counters[index]; 
    }

    void setCounter(unsigned int index, Counter counter) 
    {
        this->counters[index] = counter;
    }

    std::array<Fifo, F> getFifos() const
    {
        return this->fifos; 
    }

    Fifo getFifo(unsigned int index) const
    {
        return this->fifos[index]; 
    }

    void setFifo(unsigned int index, Fifo fifo) 
    {
        this->fifos[index] = fifo; 
    }

    void addOffsetToFifo(unsigned int index)
    {
        this->fifos[index].addOffset();
    }

    void addOffsetToCounter(unsigned int index)
    {
        this->counters[index].addOffset();
    }

};

enum TransitionType { Character, CharacterClass, Wildcard, Backreference};

template <size_t T, size_t C, size_t F>
class Transition {
    protected:
    const TransitionType type;
    const std::array<int, T> tokens;
    const State<C, F>* current;
    State<C, F>* next;
    int stream_increment;

    /* Additional Info */
    const bool at_start;
    const bool at_end;

    /* Type Specific */
    const bool negated;

    Transition(TransitionType type, const std::array<int, T> tokens, State<C, F> *current, State<C, F> *next, int stream_increment, bool at_start, bool at_end, bool negated = false) : type(type), tokens(tokens), current(current), next(next), stream_increment(stream_increment), at_start(at_start), at_end(at_end), negated(negated)
    { }

    private:
    bool testStartAnchor(int index, const char *stream, bool multiline) const
    {
        if (multiline)
            return index == 0 || stream[index - 1] == '\n';
        else
            return index == 0;
    }

    bool testEndAnchor(int index, const char *stream, int stream_size, bool multiline) const
    {
        if (multiline)
            return index == stream_size || stream[index] == '\n';
        else
            return index == stream_size;
    }

    bool testTokens(std::array<int, T> tokens, const char *stream, bool insensitive) const
    {
        bool has_match = false;

        for (size_t i = 0; i < T && !has_match; i++)
        {
            int token = tokens.at(i);
            has_match = this->testToken(token, stream, insensitive);
        }

        return (this->negated && !has_match) || (!this->negated && has_match);
    }

    bool testToken(int token, const char *stream, bool insensitive) const
    {
        int stream_token = insensitive ? toupper(stream[this->current->getStreamIndex()]) : stream[this->current->getStreamIndex()];
        if (insensitive)
            token = toupper(token);

        return token == stream_token;
    }

    void transferCounters() const
    {
        for (size_t i = 0; i < this->current->getCounters().size(); i++)
            this->next->setCounter(i, this->current->getCounter(i));
    }

    void transferFifos() const
    {
        for(size_t i = 0; i < this->current->getFifos().size(); i++)
            this->next->setFifo(i, this->current->getFifo(i));
    }

    public:
    const State<C, F> *getCurrent() const
    {
        return this->current;
    }

    const State<C, F> *getNext() const
    {
        return this->next;
    }

    void execute() const
    {
        this->next->setStreamIndex(this->current->getStreamIndex() + this->stream_increment);        
        this->next->setActive(true);
        this->transferCounters();
        this->transferFifos();
    }

    template<size_t N>
    void updateCounters(std::array<int, N> counter_indices, std::array<CounterOperation, N> operations, State<C, F> *target_current) const
    {
        for(size_t i = 0; i < N; i++)
        {
            if (operations[i] == SET && target_current->isActive())
                target_current->addOffsetToCounter(counter_indices[i]);

            Counter c = this->current->getCounter(counter_indices[i]);
            c.execute(operations[i], !target_current->isActive());
            this->next->setCounter(counter_indices[i], c);
        }
    }

    void addToFifo(int code_point, int fifo_index, bool clear, State<C, F> *target_current) const
    {
        if (clear && target_current->isActive())
            target_current->addOffsetToFifo(fifo_index);

        Fifo f = this->current->getFifo(fifo_index);
        f.enqueue(code_point, clear && !target_current->isActive());
        this->next->setFifo(fifo_index, f);
    }

    void addFifoToFifo(int src_fifo_index, int dest_fifo_index, bool clear, State<C, F> *target_current) const
    {
        if (clear && target_current->isActive())
            target_current->addOffsetToFifo(dest_fifo_index);

        Fifo src_fifo = this->current->getFifo(src_fifo_index);
        Fifo dest_fifo = this->current->getFifo(dest_fifo_index);
        dest_fifo.enqueueFifo(src_fifo, clear && !target_current->isActive());
        this->next->setFifo(dest_fifo_index, dest_fifo);
    }

    bool test(const char *stream, int stream_size, bool multiline, bool insensitive, bool singleline) const
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

    template <size_t N>
    bool testCounters(std::array<int, N> counter_indices, std::array<CounterCheck, N> checks) const
    {
        for(size_t i = 0; i < N; i++)
        {
            Counter c = this->getCurrent()->getCounter(counter_indices[i]);
            if (!c.check(checks[i]))
                return false;
        }

        return true;
    }

    bool testFifo(const char *stream, int stream_size, int fifo_index, bool multiline, bool insensitive)
    {
        if (this->type != TransitionType::Backreference)
            return false;

        if (!this->current->isActive() || this->current->getStreamIndex() > stream_size)
            return false;

        if (this->at_start && !this->testStartAnchor(this->current->getStreamIndex(), stream, multiline))
            return false;

        if (!this->current->getFifo(fifo_index).checkQueue(stream, stream_size, this->current->getStreamIndex(), insensitive, &this->stream_increment))
            return false;
        
        if (this->at_end && !this->testEndAnchor(this->current->getStreamIndex() + this->stream_increment, stream, stream_size, multiline))
            return false;

        return true;
    }
};

template <size_t C, size_t F>
class CharacterTransition : public Transition<1, C, F> {
    public:
    CharacterTransition(int token, State<C, F> *current, State<C, F> *next, bool at_start, bool at_end) : Transition<1, C, F>(TransitionType::Character, std::array<int, 1>({token}), current, next, 1, at_start, at_end)
    { }
};

template <size_t T, size_t C, size_t F>
class CharacterClassTransition : public Transition<T, C, F> {
    public:
    CharacterClassTransition(const std::array<int, T> tokens, bool negated, State<C, F> *current, State<C, F> *next, bool at_start, bool at_end) : Transition<T, C, F>(TransitionType::CharacterClass, tokens, current, next, 1, at_start, at_end, negated)
    { }
};

template <size_t C, size_t F>
class WildcardTransition : public Transition<0, C, F> {
    public:
    WildcardTransition(State<C, F> *current, State<C, F> *next, bool at_start, bool at_end) : Transition<0, C, F>(TransitionType::Wildcard, std::array<int, 0>(), current, next, 1, at_start, at_end)
    { }
};

template <size_t C, size_t F>
class BackreferenceTransition : public Transition<0, C, F> {
    public:
    BackreferenceTransition(State<C, F> *current, State<C, F> *next, bool at_start, bool at_end) : Transition<0, C, F>(TransitionType::Backreference, std::array<int, 0>(), current, next, 0, at_start, at_end)
    { }
};

<#list automata as automaton>
<#assign template_arguments = "${automaton.counter_ids?size}, ${automaton.fifo_ids?size}">
//kernel for /${automaton.expression}/
int kernel${automaton?counter}(char stream[], int stream_size)
{
    State<${template_arguments}> current[${automaton.states?size}];
    State<${template_arguments}> next[${automaton.states?size}];

    <#assign transition_counter = 0>
    <#list automaton.transitions as transition>
      <#if transition.type == "Character">
    CharacterTransition<${template_arguments}> t${transition_counter}(${transition.token}, &current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      <#elseif transition.type == "CharacterClass">
    CharacterClassTransition<${transition.tokens?size}, ${template_arguments}> t${transition_counter}(std::array<int, ${transition.tokens?size}>({${transition.tokens?join(", ")}}), ${transition.negated?c}, &current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      <#elseif transition.type == "Wildcard">
    WildcardTransition<${template_arguments}> t${transition_counter}(&current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      <#elseif transition.type == "Backreference">
    BackreferenceTransition<${template_arguments}> t${transition_counter}(&current[${transition.source.id}], &next[${transition.target.id}], ${transition.at_start?c}, ${transition.at_end?c});
      </#if>
    <#assign transition_counter += 1>
    </#list>

    int transitions_occurred;
    bool in_end_state = false;

    for (int stream_offset = 0; stream_offset < stream_size; stream_offset++) 
    {
        memset(current, 0, ${automaton.states?size} * sizeof(State<${template_arguments}>));
        current[${automaton.start_state.id}].setActive(true);
        current[${automaton.start_state.id}].setStreamIndex(stream_offset);

        do {
            transitions_occurred = 0;
     
        <#assign transition_counter = 0>
        <#list automaton.transitions as transition>
        <#assign callTest = "">
        <#if transition.type == "Backreference">
          <#assign callTest = "t${transition_counter}.testFifo(stream, stream_size, ${transition.fifo_to_match.id_no}, ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c})">
        <#else>
          <#assign callTest = "t${transition_counter}.test(stream, stream_size, ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c}, ${hasSinglelineFlag(automaton)?c})">
        </#if>
            <#assign counter_arguments = "">
            if (${callTest}<#if transition.counters_info?size != 0> && t${transition_counter}.testCounters<${transition.counters_info?size}>({{<#list transition.counters_info as counter_info>${counter_info.counter.id_no}<#if !counter_info?is_last>, </#if></#list>}}, {{<#list transition.counters_info as counter_info>{CounterOperation::${counter_info.operation}, ${counter_info.counter.target_value1}, ${counter_info.counter.target_value2}}<#if !counter_info?is_last>, </#if></#list>}})</#if>) 
            { 
                transitions_occurred++;
                t${transition_counter}.execute();
                <#if transition.counters_info?size != 0>
                t${transition_counter}.updateCounters<${transition.counters_info?size}>({{<#list transition.counters_info as counter_info>${counter_info.counter.id_no}<#if !counter_info?is_last>, </#if></#list>}}, {{<#list transition.counters_info as counter_info>CounterOperation::${counter_info.operation}<#if !counter_info?is_last>, </#if></#list>}}, &current[${transition.target.id}]);
                </#if>
                <#list transition.fifos_info as fifo_info>
                <#if transition.type == "Backreference">
                t${transition_counter}.addFifoToFifo(${transition.fifo_to_match.id_no}, ${fifo_info.fifo.id_no}, ${fifo_info.clear?c}, &current[${transition.target.id}]);
                <#else>
                t${transition_counter}.addToFifo(stream[t${transition_counter}.getCurrent()->getStreamIndex()], ${fifo_info.fifo.id_no}, ${fifo_info.clear?c}, &current[${transition.target.id}]);
                </#if>
                </#list>
            }
        <#assign transition_counter += 1>
        </#list>

            if (transitions_occurred)
            {
                memcpy(current, next, ${automaton.states?size} * sizeof(State<${template_arguments}>));
                memset(next, 0, ${automaton.states?size} * sizeof(State<${template_arguments}>));
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

<#function hasCaseInsensitiveFlag automaton>
  <#return automaton.flags?contains("i")>
</#function>

<#function hasMultilineFlag automaton>
  <#return automaton.flags?contains("m")>
</#function>

<#function hasSinglelineFlag automaton>
  <#return automaton.flags?contains("s")>
</#function>