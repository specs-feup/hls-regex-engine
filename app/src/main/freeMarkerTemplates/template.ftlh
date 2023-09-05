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

<#function getStateComparisons state group>
    <#assign result = "">
        <#list group.transitions as transition>
            <#if !transition.wildcard>
                <#assign result = result + "c ${getComparator(transition)} ${transition.token}">
            <#else>
                <#assign result = result + "true"> 
            </#if>

            <#if !transition?is_last>
                <#assign result = result + getJoinOperator(group)> 
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

    for (int i = 0; i < stream_size; i++)
    {
        char c = stream[i];
        <#list states as state>
        <#list state.transition_groups as group>
        if ((${getStateComparisons(state, group)}) && curr[${state.id}])
            next[${group.transitions[0].target.id}] = 1;
        </#list>
        </#list>
            
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