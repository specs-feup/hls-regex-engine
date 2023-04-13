// Kernel generated for regex \${raw_regex}\

#include <stdio.h>
#include <string.h>

int match = 0;

void kernel(char stream[], int stream_size)
{
    int curr[${total_states}] = {0};
    int next[${total_states}] = {0};
    curr[${start_state.id}] = 1;

    for (int i = 0; i < stream_size; i++)
    {
        char c = stream[i];
        <#list states as state>
        <#list state.transitions as transition>
        if (c == '${transition.token}' && curr[${state.id}])
            next[${transition.target.id}] = 1;
        </#list>
        </#list>
            
        memcpy(curr, next, ${total_states} * sizeof(int));
        memset(next, 0, ${total_states} * sizeof(int));
    }

    if (curr[${end_states[0].id}]<#list end_states[1..] as end> || curr[${end.id}]</#list>)
    {
        match = 1;
        return;
    }
}

int main()
{
    char stream[256];
    while (1)
    {
        match = 0;
        printf("Insert a string: ");
        scanf("%s", stream);
        kernel(stream, strlen(stream));
        match ? printf("\nIt's a match! :)\n\n") : printf("\nIt's not a match :(\n\n");
    }

    return 0;
}