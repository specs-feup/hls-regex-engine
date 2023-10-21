#include <stdio.h>
#include <string.h>
#include <array>
#include <ctype.h>

struct Counter {
    unsigned int value = 0;
    unsigned int value_offset = 0;
};

bool counterCheckEqual(const Counter &counter, const unsigned int target_value)
{
    bool valid = false;
    for (unsigned int i = 0; i <= counter.value_offset && !valid; i++)
        valid = (counter.value - i + 1) == target_value;

    return valid;
}

bool counterCheckLess(const Counter &counter, const unsigned int target_value)
{
    bool valid = false;
    for (unsigned int i = 0; i <= counter.value_offset && !valid; i++)
        valid = (counter.value - i + 1) < target_value;

    return valid;
}

bool counterCheckEqualMore(const Counter &counter, const unsigned int target_value)
{
    bool valid = false;
    for (unsigned int i = 0; i <= counter.value_offset && !valid; i++)
        valid = (counter.value - i + 1) >= target_value;

    return valid;
}

bool counterCheckRange(const Counter &counter, const unsigned int lower_bound, const unsigned int upper_bound)
{
    bool valid = false;
    for (unsigned int i = 0; i <= counter.value_offset && !valid; i++)
    {
        const unsigned int checked_value = counter.value - i + 1;
        valid = checked_value >= lower_bound && checked_value <= upper_bound;
    }

    return valid;
}

void counterSet(Counter *counter)
{
    counter->value = 1;
    counter->value_offset = 0;
}

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

        for (unsigned int i = 0; i <= this->allowed_offsets; i++)
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
    int stream_index;
    std::array<Counter, C> counters;
    std::array<Fifo, F> fifos;

    public:
    State() : stream_index(-1), counters({}), fifos({}) 
    { }

    bool isActive() const
    { 
        return this->stream_index >= 0; 
    }

    int getStreamIndex() const
    {
        return this->stream_index; 
    }

    void setStreamIndex(int stream_index) 
    {
        this->stream_index = stream_index; 
    }

    Counter* getCounter(unsigned int index)
    {
        return &this->counters[index]; 
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
};

bool testStartAnchor(int index, const char *stream, bool multiline)
{
    if (multiline)
        return index == 0 || stream[index - 1] == '\n';
    else
        return index == 0;
}

bool testEndAnchor(int index, const char *stream, int stream_size, bool multiline)
{
    if (multiline)
        return index == stream_size || stream[index] == '\n';
    else
        return index == stream_size;
}

template <size_t C, size_t F>
void transferCounters(State<C,F> *from, State<C,F> *to)
{
    for(size_t i = 0; i < C; i++)
        to->setCounter(i, *from->getCounter(i));
}

<#list automata as automaton>
<#assign template_arguments = "${automaton.counter_ids?size}, ${automaton.fifo_ids?size}">
//kernel for /${automaton.expression}/
bool kernel${automaton?counter}(char stream[], int stream_size)
{
    State<${template_arguments}> current[${automaton.states?size}];
    State<${template_arguments}> next[${automaton.states?size}];

    int transitions_occurred;
    bool in_end_state = false;

    for (int stream_offset = 0; stream_offset < stream_size; stream_offset++) 
    {
        for (int i = 0; i < ${automaton.states?size}; i++)
            current[i] = State<${template_arguments}>();
        current[${automaton.start_state.id}].setStreamIndex(stream_offset);

        do {
            transitions_occurred = 0;

            <#list automaton.states as state>
            <#if (state.outgoing_transitions?size > 0)>
            if (current[${state.id}].isActive() && current[${state.id}].getStreamIndex() < stream_size)
            {
                int stream_token = stream[current[${state.id}].getStreamIndex()];
                <#list state.outgoing_transitions as transition>
                ${getTransitionComparison(transition, automaton)}
                {
                    transitions_occurred++;
                    ${getTransitionBody(transition)}
                }
                </#list>
            }
            </#if>
            </#list>

            if (transitions_occurred)
            {
                memcpy(current, next, ${automaton.states?size} * sizeof(State<${template_arguments}>));
                for (int i = 0; i < ${automaton.states?size}; i++)
                    next[i] = State<${template_arguments}>();
                in_end_state = <#list automaton.end_states as end_state>current[${end_state.id}].isActive()<#if !end_state?is_last> || </#if></#list>;
            }
      
        } while (transitions_occurred && !in_end_state);

        if (in_end_state)
            return true;
    }

    return false;
}

</#list>
bool runAutomata(char stream[], int stream_size)
{
    return <#list automata as automaton>kernel${automaton?counter}(stream, stream_size)<#if !automaton?is_last> || </#if></#list>;
}

int main()
{
    char stream[256];
    while (1)
    {
        printf("Insert a string: ");
        fgets(stream, 256, stdin);
        if ((strlen(stream) > 0) && (stream[strlen(stream) - 1] == '\n'))
            stream[strlen (stream) - 1] = '\0';
        bool match = runAutomata(stream, strlen(stream));
        match ? printf("\n\033[0;32mIt's a match! :)\n\n\033[0m") : printf("\n\033[0;31mIt's not a match :(\n\n\033[0m");
    }

    return 0;
}

<#function getTransitionComparison transition automaton>
    <#assign token_comp = getTokenComparison(transition, automaton)>
    <#assign anchor_comp = getAnchorComparison(transition, automaton)>
    <#assign counter_comp = getCounterComparison(transition, automaton)>
    <#assign result = "if (${token_comp})">
    <#if anchor_comp?has_content>
        <#assign result = result + "\n\t\t\t\tif(${anchor_comp})">
    </#if>
    <#if counter_comp?has_content>
        <#assign result = result + "\n\t\t\t\tif(${counter_comp})">
    </#if>
    <#return result>
</#function>

<#function getAnchorComparison transition automaton>
    <#assign result = "">
    <#if transition.at_start>
        <#assign result = result + "testStartAnchor(current[${transition.source.id}].getStreamIndex(), stream, ${hasMultilineFlag(automaton)?c})">
    </#if>
    <#if transition.at_end>
        <#if result?has_content>
            <#assign result = result + " && "?no_esc>
        </#if>
        <#assign result = result + "testEndAnchor(current[${transition.source.id}].getStreamIndex() + stream_increment, stream, stream_size, ${hasMultilineFlag(automaton)?c})">
    </#if>
    <#return result>
</#function>

<#function getCounterComparison transition automaton>
    <#assign result = "">
    <#list transition.counters_info as counter_info>
        <#assign current_counter = "*current[${transition.source.id}].getCounter(${counter_info.counter.id_no})">
        <#if counter_info.operation == "COMPARE_EQUAL">
            <#assign result = result + "counterCheckEqual(${current_counter}, ${counter_info.counter.target_value1})">
        <#elseif counter_info.operation == "COMPARE_LESS">
            <#assign result = result + "counterCheckLess(${current_counter}, ${counter_info.counter.target_value1})">
        <#elseif counter_info.operation == "COMPARE_EQUALMORE">
            <#assign result = result + "counterCheckEqualMore(${current_counter}, ${counter_info.counter.target_value1})">
        <#elseif counter_info.operation == "COMPARE_RANGE">
            <#assign result = result + "counterCheckRange(${current_counter}, ${counter_info.counter.target_value1}, ${counter_info.counter.target_value2})">
        </#if>
        <#if !counter_info?is_last && transition.counters_info[counter_info?index + 1].operation != "SET"><#assign result = result + " && "?no_esc></#if>
    </#list>
    <#return result>
</#function>

<#function getTokenComparison transition automaton>
    <#if transition.type == "Character">
        <#return "int stream_increment = stream_token == ${transition.token}">
    <#elseif transition.type == "CharacterClass">
        <#assign comparator = "=="?no_esc>
        <#assign separator = "||"?no_esc>
        <#if transition.negated>
            <#assign comparator = "!="?no_esc>
            <#assign separator = "&&"?no_esc>           
        </#if>
        <#assign result = "int stream_increment = (">
        <#list transition.tokens as token>
            <#assign result = result + " stream_token ${comparator} ${token?c}">
            <#if !token?is_last>
            <#assign result = result + " ${separator}">
            </#if>
        </#list>
        <#return result + ")">
    <#elseif transition.type == "Wildcard">
        <#if hasSinglelineFlag(automaton)>
            <#return "int stream_increment = 1">
        <#else>
            <#return "int stream_increment = stream_token != '\\n'"?no_esc>
        </#if>
    <#elseif transition.type == "Backreference">
        <#return "TODO">
    </#if>
</#function>

<#function getTransitionBody transition>
    <#assign default_body = getDefaultBody(transition)>
    <#assign counter_body = getCounterBody(transition)>
    <#assign result = default_body>
    <#if counter_body?has_content>
        <#assign result = result + "\n\t\t\t\t\t" + counter_body>
    </#if>
    <#return result>
</#function>

<#function getDefaultBody transition>
    <#assign result = "next[${transition.target.id}].setStreamIndex(current[${transition.source.id}].getStreamIndex() + stream_increment);">
    <#assign result = result + "\n\t\t\t\t\ttransferCounters(&current[${transition.source.id}], &next[${transition.target.id}]);"?no_esc>
    <#return result>
</#function>

<#function getCounterBody transition>
    <#assign result = "">
    <#list transition.counters_info as counter_info>
        <#assign target_current = "current[${transition.target.id}]"> 
        <#assign current_counter = "current[${transition.source.id}].getCounter(${counter_info.counter.id_no})">
        <#assign next_counter = "next[${transition.target.id}].getCounter(${counter_info.counter.id_no})">
        <#if counter_info.operation == "SET">
            <#assign result = result + "if (${target_current}.isActive()){${target_current}.getCounter(${counter_info.counter.id_no})->value_offset++; ${next_counter}->value = ${current_counter}->value + 1;}else{counterSet(${next_counter});}"?no_esc>
        <#else>
            <#assign result = result + "${next_counter}->value = ${current_counter}->value + 1;"?no_esc>
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