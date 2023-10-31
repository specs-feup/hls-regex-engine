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

void counterSet(Counter &counter)
{
    counter.value = 1;
    counter.value_offset = 0;
}

struct Fifo {
    std::array<int, 32> data = {};
    size_t size = 0;
    unsigned int allowed_offsets = 0;
};

void fifoClear(Fifo &fifo) 
{
    fifo.size = 0;
    fifo.allowed_offsets = 0;
}

void fifoEnqueue(Fifo &fifo, int element, bool clear)
{
    if (clear)
        fifoClear(fifo);
    fifo.data[fifo.size++] = element;
}

void fifoEnqueueFifo(Fifo &dest, const Fifo &source, bool clear)
{
    for (size_t i = 0; i < source.size; i++)
        fifoEnqueue(dest, source.data[i], clear && i == 0);
}

bool fifoCheck(const Fifo &fifo, const char *stream, int stream_size, int stream_index, int *stream_increment)
{
    if ((stream_index == stream_size && fifo.size != 0) || stream_index > stream_size)
        return false;

    for (unsigned int i = 0; i <= fifo.allowed_offsets; i++)
    {
        bool valid = true;
        for(int j = 0; j + i < fifo.size && valid; j++)
            valid = stream[stream_index + j] == fifo.data[j + i];

        if (valid)
        {
            *stream_increment = fifo.size - i;
            return true;
        }
    }

    return false;
}

template <size_t C, size_t F>
struct State {
    int stream_index = -1;
    std::array<Counter, C> counters = {};
    std::array<Fifo, F> fifos = {};
};

template <size_t C, size_t F>
bool stateIsActive(const State<C,F> &state)
{ 
    return state.stream_index >= 0; 
}

template <size_t C, size_t F>
void transferCounters(State<C,F> &from, State<C,F> &to)
{
    to.counters = from.counters;
}

template <size_t C, size_t F>
void transferFifos(State<C,F> &from, State<C,F> &to)
{
    to.fifos = from.fifos;
}

bool testStartAnchor(int index)
{
    return index == 0;
}

bool testStartAnchorMultiline(int index, const char *stream)
{
    return index == 0 || stream[index - 1] == '\n';
}

bool testEndAnchor(int index, int stream_size)
{
    return index == stream_size;
}

bool testEndAnchorMultiline(int index, const char *stream, int stream_size)
{
    return index == stream_size || stream[index] == '\n';
}

int toUpper(int token)
{
    if (token >= 'a' && token <= 'z')
        return token - 32;
    return token;
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
        current[${automaton.start_state.id}].stream_index = stream_offset;

        do {
            transitions_occurred = 0;

            <#list automaton.states as state>
            const bool ${currentVarActive(state.id)} = stateIsActive(current[${state.id}]);
            </#list>

            <#list automaton.states as state>
            <#if (state.outgoing_transitions?size > 0)>
            if (${currentVarActive(state.id)})
            {
                const int stream_token = ${getTransitionToken("stream[current[${state.id}].stream_index]", automaton)};
                int stream_increment;
                <#list state.outgoing_transitions as transition>
                ${getTransitionComparison(transition, automaton)}
                {
                    transitions_occurred++;
                    ${getTransitionBody(transition, automaton)}
                }
                </#list>
            }
            </#if>
            </#list>

            in_end_state = <#list automaton.end_states as end_state>${currentVarActive(end_state.id)}<#if !end_state?is_last> || </#if></#list>;
            memcpy(current, next, ${automaton.states?size} * sizeof(State<${template_arguments}>));
            for (int i = 0; i < ${automaton.states?size}; i++)
                next[i] = State<${template_arguments}>();
      
        } while (transitions_occurred && !in_end_state);

        if (in_end_state)
            return true;
    }

    return false;
}

</#list>
bool runAutomata(char stream[], int stream_size)
{
    #pragma HLS interface s_axilite port=stream
    <#list automata as automaton>const bool k${automaton?counter} = kernel${automaton?counter}(stream, stream_size);</#list>
    return <#list automata as automaton>k${automaton?counter}<#if !automaton?is_last> || </#if></#list>;
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

<#function currentVarActive id>
    <#return "is_current${id}_active">
</#function>

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
        <#if hasMultilineFlag(automaton)>
            <#assign result = result + "testStartAnchorMultiline(current[${transition.source.id}].stream_index, stream)">
        <#else>
            <#assign result = result + "testStartAnchor(current[${transition.source.id}].stream_index)">
        </#if>
    </#if>
    <#if transition.at_end>
        <#if result?has_content>
            <#assign result = result + " && "?no_esc>
        </#if>
        <#if hasMultilineFlag(automaton)>
            <#assign result = result + "testEndAnchorMultiline(current[${transition.source.id}].stream_index + stream_increment, stream, stream_size)">
        <#else>
            <#assign result = result + "testEndAnchor(current[${transition.source.id}].stream_index + stream_increment, stream_size)">
        </#if>
    </#if>
    <#return result>
</#function>

<#function getCounterComparison transition automaton>
    <#assign result = "">
    <#list transition.counters_info as counter_info>
        <#assign current_counter = "current[${transition.source.id}].counters[${counter_info.counter.id_no}]">
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

<#function getTransitionToken token automaton>
    <#if hasCaseInsensitiveFlag(automaton)>
        <#return "toUpper(${token})">
    <#else>
        <#return "${token}">
    </#if>
</#function>

<#function getTokenComparison transition automaton>  
    <#if transition.type == "Character">
        <#return "(stream_increment = stream_token == ${getTransitionToken(transition.token, automaton)})">
    <#elseif transition.type == "CharacterClass">
        <#assign result = "">
        <#assign comparator = "=="?no_esc>
        <#assign separator = "||"?no_esc>
        <#if transition.negated>
            <#assign comparator = "!="?no_esc>
            <#assign separator = "&&"?no_esc>
            <#assign result = result + "current[${transition.source.id}].stream_index < stream_size && "?no_esc>           
        </#if>
        <#assign result = result + "(stream_increment = (">
        <#list transition.tokens as token>
            <#assign result = result + "stream_token ${comparator} ${getTransitionToken(token?c, automaton)}">
            <#if !token?is_last>
            <#assign result = result + " ${separator}">
            </#if>
        </#list>
        <#return result + "))">
    <#elseif transition.type == "Wildcard">
        <#if hasSinglelineFlag(automaton)>
            <#return "current[${transition.source.id}].stream_index < stream_size && (stream_increment = 1)"?no_esc>
        <#else>
            <#return "current[${transition.source.id}].stream_index < stream_size && (stream_increment = stream_token != '\\n')"?no_esc>
        </#if>
    <#elseif transition.type == "Backreference">
        <#assign backreference_fifo = "current[${transition.source.id}].fifos[${transition.fifo_to_match.id_no}]">
        <#return "fifoCheck(${backreference_fifo}, stream, stream_size, current[${transition.source.id}].stream_index, &stream_increment)"?no_esc>
    </#if>
</#function>

<#function getTransitionBody transition automaton>
    <#assign default_body = getDefaultBody(transition, automaton)>
    <#assign counter_body = getCounterBody(transition)>
    <#assign fifo_body = getFifoBody(transition)>
    <#assign result = default_body>
    <#if counter_body?has_content>
        <#assign result = result + "\n\t\t\t\t\t" + counter_body>
    </#if>
    <#if fifo_body?has_content>
        <#assign result = result + "\n\t\t\t\t\t" + fifo_body>
    </#if>
    <#return result>
</#function>

<#function getDefaultBody transition automaton>
    <#assign result = "next[${transition.target.id}].stream_index = current[${transition.source.id}].stream_index + stream_increment;">
    <#if automaton.counter_ids?size != 0>
        <#assign result = result + "\n\t\t\t\t\ttransferCounters(current[${transition.source.id}], next[${transition.target.id}]);"?no_esc>
    </#if>
    <#if automaton.fifo_ids?size != 0>
        <#assign result = result + "\n\t\t\t\t\ttransferFifos(current[${transition.source.id}], next[${transition.target.id}]);"?no_esc>
    </#if>
    <#return result>
</#function>

<#function getCounterBody transition>
    <#assign result = "">
    <#list transition.counters_info as counter_info>
        <#assign target_current = "current[${transition.target.id}]"> 
        <#assign current_counter = "current[${transition.source.id}].counters[${counter_info.counter.id_no}]">
        <#assign next_counter = "next[${transition.target.id}].counters[${counter_info.counter.id_no}]">
        <#if counter_info.operation == "SET">
            <#assign result = result + "if (${currentVarActive(transition.target.id)}){\n\t\t\t\t\t\t${target_current}.counters[${counter_info.counter.id_no}].value_offset++; ${next_counter}.value++;\n\t\t\t\t\t}else { counterSet(${next_counter}); }"?no_esc>
        <#else>
            <#assign result = result + "${next_counter}.value++;"?no_esc>
        </#if>
    </#list>
    <#return result>
</#function>

<#function getFifoBody transition>
    <#assign result = "">
    <#list transition.fifos_info as fifo_info>
        <#assign target_current = "current[${transition.target.id}]"> 
        <#assign current_fifo = "current[${transition.source.id}].fifos[${fifo_info.fifo.id_no}]">
        <#assign next_fifo = "next[${transition.target.id}].fifos[${fifo_info.fifo.id_no}]">
        <#if transition.type == "Backreference">
            <#assign source_fifo = "current[${transition.source.id}].fifos[${transition.fifo_to_match.id_no}]">
            <#if fifo_info.clear>
                <#assign result = result + "if (${currentVarActive(transition.target.id)}){\n\t\t\t\t\t\t${target_current}.fifos[${fifo_info.fifo.id_no}].allowed_offsets++;fifoEnqueueFifo(${next_fifo}, ${source_fifo}, false);\n\t\t\t\t\t}else{ fifoEnqueueFifo(${next_fifo}, ${source_fifo}, true); }"?no_esc>
            <#else>
                <#assign result = result + "fifoEnqueueFifo(${next_fifo}, ${source_fifo}, false);">
            </#if>
        <#else>
            <#if fifo_info.clear>
                <#assign result = result + "if (${currentVarActive(transition.target.id)}){\n\t\t\t\t\t\t${target_current}.fifos[${fifo_info.fifo.id_no}].allowed_offsets++;fifoEnqueue(${next_fifo}, stream_token, false);\n\t\t\t\t\t}else{ fifoEnqueue(${next_fifo}, stream_token, true); }"?no_esc>
            <#else>
                <#assign result = result + "fifoEnqueue(${next_fifo}, stream_token, false);">
            </#if>
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