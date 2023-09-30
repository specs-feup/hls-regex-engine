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

enum TransitionType { Character, CharacterClass, Wildcard };
enum AnchorType { NONE, START, END };

template <size_t N>
class Transition {
    protected:
    TransitionType type;
    std::array<int, N> tokens;
    State* current;
    State* next;
    int stream_increment;

    /* Additional Info */
    AnchorType anchor_type;

    /* Type Specific */
    bool negated = false;
    bool padding = false;

    Transition(TransitionType type, const std::array<int, N> tokens, State *current, State *next, int stream_increment, AnchorType anchor_type) {
        this->type = type;
        this->tokens = tokens;
        this->current = current;
        this->next = next;
        this->stream_increment = stream_increment;
        this->anchor_type = anchor_type;
    }

    private:
    void execute()
    {
        this->next->setActive(true);
        this->next->setStreamIndex(this->current->getStreamIndex() + this->stream_increment);
    }

    bool testAnchor(char *stream, int stream_size, bool multiline)
    {
        int current_index = this->current->getStreamIndex();
        switch (this->anchor_type)
        {
            case AnchorType::START:
                if (multiline)
                    return current_index == 0 || stream[current_index - 1] == '\n';
                else
                    return current_index == 0;
            case AnchorType::END:
                if (multiline)
                    return current_index == stream_size - 1 || stream[current_index + 1] == '\n';
                else
                    return current_index == stream_size - 1;
                break;

            default:
                return true;
        }
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

        if ((this->negated && !has_match) || (!this->negated && has_match))
        {
            this->execute();
            return true;
        }
        
        return false; 
    }

    bool testToken(int token, char *stream, bool insensitive)
    {
        int stream_token = insensitive ? toupper(stream[this->current->getStreamIndex()]) : stream[this->current->getStreamIndex()];
        if (insensitive)
            token = toupper(token);

        if (token == stream_token)
        {
            this->execute();
            return true;
        }

        return false;
    }

    public:
    bool test(char *stream, int stream_size, bool multiline, bool insensitive)
    {
        if (!this->current->isActive() || this->current->getStreamIndex() >= stream_size || !this->testAnchor(stream, stream_size, multiline))
            return false;

        if (this->type == TransitionType::Character)
        {
            return this->testToken(this->tokens[0], stream, insensitive);
        }
        else if (this->type == TransitionType::CharacterClass)
        {
            return this->testTokens(this->tokens, stream, insensitive);
        }
        else if (this->type == TransitionType::Wildcard)
        {
            this->execute();
            return true;
        }

        return this->negated;
    }
};

class CharacterTransition : public Transition<1> {
    public:
    CharacterTransition(int token, State *current, State *next, AnchorType anchor_type) : Transition(TransitionType::Character, std::array<int, 1>({token}), current, next, 1, anchor_type)
    { }
};

template <size_t N>
class CharacterClassTransition : public Transition<N> {
    public:
    CharacterClassTransition(const std::array<int, N> tokens, bool negated, State *current, State *next, AnchorType anchor_type) : Transition<N>(TransitionType::CharacterClass, tokens, current, next, 1, anchor_type)
    { 
        this->negated = negated;
    }
};

class WildcardTransition : public Transition<0> {
    public:
    WildcardTransition(bool padding, State *current, State *next, AnchorType anchor_type) : Transition(TransitionType::Wildcard, std::array<int, 0>(), current, next, 1, anchor_type)
    { 
        this->padding = padding;
    }
};

<#list automata as automaton>
//kernel for /${automaton.expression}/
int kernel${automaton?counter}(char stream[], int stream_size)
{
    State current[${automaton.states?size}];
    State next[${automaton.states?size}];
    current[${automaton.start_state.id}].setActive(true);

    <#assign transition_counter = 0>
    <#list automaton.transitions as transition>
      <#if transition.type == "Character">
    CharacterTransition t${transition_counter}(${transition.token}, &current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      <#elseif transition.type == "CharacterClass">
    CharacterClassTransition<${transition.tokens?size}> t${transition_counter}(std::array<int, ${transition.tokens?size}>({${transition.tokens?join(", ")}}), ${transition.negated?c}, &current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      <#elseif transition.type == "Wildcard">
    WildcardTransition t${transition_counter}(${transition.padding?c}, &current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      </#if>
    <#assign transition_counter += 1>
    </#list>

    int transitions_occurred;

    do {
        transitions_occurred = 0;
     
        <#assign transition_counter = 0>
        <#list automaton.transitions as transition>
        transitions_occurred += t${transition_counter}.test(stream, stream_size, ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c});
        <#assign transition_counter += 1>
        </#list>

        if (transitions_occurred)
        {
            memcpy(current, next, ${automaton.states?size} * sizeof(State));
            memset(next, 0, ${automaton.states?size} * sizeof(State));
        }
      
    } while (transitions_occurred);

    return <#list automaton.end_states as end_state>current[${end_state.id}].isActive()<#if !end_state?is_last> || </#if></#list>;
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
  <#assign result = "">

  <#if group.fifo_to_match??>
  <#assign queue_ref = "&${group.fifo_to_match.id}"?no_esc>
  <#assign result = result + "int is_valid = checkQueue(${queue_ref}, stream," + " &i);"?no_esc>
  <#assign result = result + "if (is_valid) {">
  </#if>

  <#assign result = result + "next[${group.transitions[0].target.id}] = 1;">
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
    <#assign result = result + " enqueue(&${id}, c, 0);"?no_esc>
  </#list>

  <#if group.fifo_to_match??>
  <#assign result = result + "}">
  </#if>

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