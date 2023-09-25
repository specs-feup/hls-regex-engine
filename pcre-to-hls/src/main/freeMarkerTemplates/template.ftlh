#include <stdio.h>
#include <string.h>
#include <ctype.h>

typedef struct {
    int data[64];
    int *head;
    int size;
} Queue;

void initQueue(Queue *queue) 
{
    queue->head = queue->data;
    queue->size = 0;
}

void enqueue(Queue *queue, int item) 
{
    *(queue->data + queue->size++) = item;
}

int dequeue(Queue *queue) 
{
    int ret = *queue->head;
    queue->head++;
    return ret;
}

<#list automata as automaton>
//kernel for /${automaton.expression}/
int kernel${automaton?counter}(char stream[], int stream_size)
{
    int curr[${automaton.states?size}] = {0};
    int next[${automaton.states?size}] = {0};
    curr[${automaton.start_state.id}] = 1;
    ${declareCounters(automaton.counter_ids)}
    ${declareFifos(automaton.fifo_ids)}

    for (int i = 0; i < stream_size; i++)
    {
        char c = ${setCurrentCharVar(automaton)};
        <#list automaton.states as state>
        <#list state.transition_groups as group>
        if (curr[${state.id}] && ${getAnchorComparisons(group, automaton)}(${getCharacterComparisons(group, automaton)})${getCounterComparisons(group)})
          {${getBody(group)}}
        </#list>
        </#list>
        
        ${resetCounterGuards(automaton.counter_ids)}
        memcpy(curr, next, ${automaton.states?size} * sizeof(int));
        memset(next, 0, ${automaton.states?size} * sizeof(int));
    }

    return <#list automaton.end_states as end_state>curr[${end_state.id}]<#if !end_state?is_last> || </#if></#list>;
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
<#function getComparator transition>
  <#if transition.negated>
    <#return "!=">
  <#else>
    <#return "==">
  </#if>
</#function>

<#function getJoinOperator group>
  <#if group.intercept>
    <#return " &"?no_esc+"& "?no_esc>
  <#else>
    <#return " || ">
  </#if>
</#function>

<#function getCounterComparisons group>
  <#assign result = "">
  <#list group.counter_infos as counter_info>
    <#assign id = counter_info.counter.id>
    <#assign comp_symbol = "">
    <#if counter_info.operation == "COMPARE_EQUAL">
      <#assign comp_symbol = "==">
    <#elseif counter_info.operation == "COMPARE_LESS">
      <#assign comp_symbol = "<"?no_esc>
    <#elseif counter_info.operation == "COMPARE_MORE">
      <#assign comp_symbol = ">"?no_esc>
    <#elseif counter_info.operation == "COMPARE_EQUALMORE">
      <#assign comp_symbol = ">"?no_esc+"=">
    <#elseif counter_info.operation == "COMPARE_EQUALLESS">
      <#assign comp_symbol = "<"?no_esc+"=">
    </#if>
    <#if counter_info.operation == "COMPARE_RANGE">
      <#assign result = result + " && "?no_esc + "${id}[1] + (${id}[0] ? 0 : 1) " + ">="?no_esc + " ${counter_info.counter.target_value}" + " &"?no_esc+"& "?no_esc + "${id}[1] + (${id}[0] ? 0 : 1) " + "<="?no_esc + " ${counter_info.counter.target_value2}">
    <#elseif counter_info.operation != "SET">
      <#assign result = result + " && "?no_esc +  "${id}[1] + (${id}[0] ? 0 : 1) ${comp_symbol} ${counter_info.counter.target_value}">
    </#if>

  </#list>
  <#return result>
</#function>

<#function getAnchorComparisons group automaton>
  <#assign is_line_comparison = hasMultilineFlag(automaton)>
  <#assign result = "">
  <#if group.anchor_info == "START">
    <#if is_line_comparison>
      <#assign result = "(i == 0 || stream[i - 1] == 10) && "?no_esc>
    <#else>
      <#assign result = "i == 0 && "?no_esc>
    </#if>
  <#elseif group.anchor_info == "END">
      <#if is_line_comparison>
      <#assign result = "(i == stream_size - 1 || stream[i + 1] == 10) && "?no_esc>
    <#else>
      <#assign result = "i == stream_size - 1 && "?no_esc>
    </#if>
  </#if>
  <#return result>
</#function>

<#function getCharacterComparisons group automaton>
    <#assign is_insensitive_comparison = hasCaseInsensitiveFlag(automaton)>
    <#assign is_singleline_comparison = hasSinglelineFlag(automaton)>
    <#assign result = "">
        <#list group.transitions as transition>
            <#if !transition.wildcard>
                <#assign comp_character = transition.token>
                <#if is_insensitive_comparison>
                  <#assign comp_character = "toupper(" + comp_character + ")">
                </#if>
                <#assign result = result + "c ${getComparator(transition)} ${comp_character}">
            <#else>
                <#if is_singleline_comparison || transition.padding>
                  <#assign result = result + "1"> 
                <#else>
                  <#assign result = result + "c != 10">
                </#if>
            </#if>

            <#if !transition?is_last>
                <#assign result = result + getJoinOperator(group)> 
            </#if>
        </#list>
    <#return result>
</#function>

<#function getBody group>
  <#assign result = "next[${group.transitions[0].target.id}] = 1;">
  <#list group.counter_infos as counter_info>
    <#assign id = counter_info.counter.id>
    <#if counter_info.operation != "SET">
      <#assign result = result + " if(!${id}[0]){${id}[1]++; ${id}[0] = 1;}">
    <#else>
      <#assign result = result + " ${id}[1] = 1;">
    </#if>
  </#list>

  <#list group.fifos_info as fifo_info>
    <#assign id = fifo_info.id>
    <#assign result = result + " enqueue(&${id}, c);"?no_esc>
  </#list>

  <#return result>
</#function>

<#function declareCounters counter_ids>
  <#assign result = "">
    <#list counter_ids as counter_id>
      <#assign result = result + "int ${counter_id}[] = {0, 0};">
       <#if !counter_id?is_last>
        <#assign result = result + "\n\t\t">
       </#if>
    </#list>
  <#return result>
</#function>

<#function declareFifos fifo_ids>
  <#assign result = "">
    <#list fifo_ids as fifo_id>
      <#assign result = result + "Queue ${fifo_id}; initQueue(&${fifo_id});"?no_esc>
       <#if !fifo_id?is_last>
        <#assign result = result + "\n\t\t">
       </#if>
    </#list>
  <#return result>
</#function>

<#function resetCounterGuards counter_ids>
  <#assign result = "">
    <#list counter_ids as counter_id>
      <#assign result = result + "${counter_id}[0] = 0;">
       <#if !counter_id?is_last>
        <#assign result = result + "\n\t\t\t\t">
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

<#function setCurrentCharVar automaton>
  <#if hasCaseInsensitiveFlag(automaton)>
    <#return "toupper(stream[i])">
  <#else>
    <#return "stream[i]">
  </#if>
</#function>