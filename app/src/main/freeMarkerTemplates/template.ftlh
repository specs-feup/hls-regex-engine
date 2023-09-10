// Kernel generated for regex \${raw_regex}\

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
  <#if group.counter_info??>
    <#assign id = group.counter_info.counter.id>
    <#assign comp_symbol = "">
    <#if group.counter_info.operation == "COMPARE_EQUAL">
      <#assign comp_symbol = "==">
    <#elseif group.counter_info.operation == "COMPARE_LESS">
      <#assign comp_symbol = "<"?no_esc>
    <#elseif group.counter_info.operation == "COMPARE_MORE">
      <#assign comp_symbol = ">"?no_esc>
    <#elseif group.counter_info.operation == "COMPARE_EQUALMORE">
      <#assign comp_symbol = ">"?no_esc+"=">
    <#elseif group.counter_info.operation == "COMPARE_EQUALLESS">
      <#assign comp_symbol = "<"?no_esc+"=">
    </#if>
    <#if group.counter_info.operation == "COMPARE_RANGE">
      <#assign result = result + " && "?no_esc + "${id}[1] + (${id}[0] ? 0 : 1) " + ">="?no_esc + " ${group.counter_info.counter.target_value}" + " &"?no_esc+"& "?no_esc + "${id}[1] + (${id}[0] ? 0 : 1) " + "<="?no_esc + " ${group.counter_info.counter.target_value2}">
    <#elseif group.counter_info.operation != "SET">
      <#assign result = result + " && "?no_esc +  "${id}[1] + (${id}[0] ? 0 : 1) ${comp_symbol} ${group.counter_info.counter.target_value}">
    </#if>
  </#if>
  <#return result>
</#function>

<#function getCharacterComparisons group>
    <#assign result = "">
        <#list group.transitions as transition>
            <#if !transition.wildcard>
                <#assign result = result + "c ${getComparator(transition)} ${transition.token}">
            <#else>
                <#assign result = result + "1"> 
            </#if>

            <#if !transition?is_last>
                <#assign result = result + getJoinOperator(group)> 
            </#if>
        </#list>
    <#return result>
</#function>

<#function getBody group>
  <#assign result = "next[${group.transitions[0].target.id}] = 1;">
  <#if group.counter_info??>
    <#assign id = group.counter_info.counter.id>
    <#if group.counter_info.operation != "SET">
      <#assign result = result + " if(!${id}[0]){${id}[1]++; ${id}[0] = 1;}">
    <#else>
      <#assign result = result + " ${id}[1] = 1;">
    </#if>
  </#if>
  <#return result>
</#function>

<#function declareCounters>
  <#assign result = "">
    <#list counter_ids as counter_id>
      <#assign result = result + "int ${counter_id}[] = {0, 0};">
       <#if !counter_id?is_last>
        <#assign result = result + "\n\t\t">
       </#if>
    </#list>
  <#return result>
</#function>

<#function resetCounterGuards>
  <#assign result = "">
    <#list counter_ids as counter_id>
      <#assign result = result + "${counter_id}[0] = 0;">
       <#if !counter_id?is_last>
        <#assign result = result + "\n\t\t\t\t">
       </#if>
    </#list>
  <#return result>
</#function>

#include <stdio.h>
#include <string.h>

int kernel(char stream[], int stream_size)
{
    int curr[${total_states}] = {0};
    int next[${total_states}] = {0};
    curr[${start_state.id}] = 1;
    ${declareCounters()}

    for (int i = 0; i < stream_size; i++)
    {
        char c = stream[i];
        <#list states as state>
        <#list state.transition_groups as group>
        if (curr[${state.id}] && (${getCharacterComparisons(group)})${getCounterComparisons(group)})
          {${getBody(group)}}
        </#list>
        </#list>
        
        ${resetCounterGuards()}
        memcpy(curr, next, ${total_states} * sizeof(int));
        memset(next, 0, ${total_states} * sizeof(int));
    }

    return (curr[${end_states[0].id}]<#list end_states[1..] as end> || curr[${end.id}]</#list>);
}

int main()
{
    char stream[256];
    while (1)
    {
        printf("Insert a string: ");
        fgets(stream, 256, stdin);
        if ((strlen(stream) > 0) && (stream[strlen (stream) - 1] == '\n'))
            stream[strlen (stream) - 1] = '\0';
        int match = kernel(stream, strlen(stream));
        match ? printf("\nIt's a match! :)\n\n") : printf("\nIt's not a match :(\n\n");
    }

    return 0;
}