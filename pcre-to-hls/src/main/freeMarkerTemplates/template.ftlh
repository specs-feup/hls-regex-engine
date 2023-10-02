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

    int checkQueue(char *stream, int stream_index, bool insensitive) {
        if (this->size == 0)
            return 0;

        bool valid = true;
        for (size_t i = 0; i < this->size && valid; i++)
        {
            char stream_char = insensitive ? toupper(stream[stream_index++]) : stream[stream_index++];
            char fifo_char = insensitive ? toupper(*(this->head + i)) : *(this->head + i);
            valid = stream_char == fifo_char;
        }

        return valid ? this->size : 0;
    }
};

enum TransitionType { Character, CharacterClass, Wildcard, Backreference};
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
    bool testStartAnchor(char *stream, bool multiline)
    {
        int current_index = this->current->getStreamIndex();
        if (multiline)
            return current_index == 0 || stream[current_index - 1] == '\n';
        else
            return current_index == 0;
    }

    bool testEndAnchor(char *stream, int stream_size, bool multiline)
    {
        int next_index = this->next->getStreamIndex();
        if (multiline)
            return next_index == stream_size || stream[next_index] == '\n';
        else
            return next_index == stream_size;
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
    State * getCurrent() {
        return this->current;
    }

    bool test(char *stream, int stream_size, bool multiline, bool insensitive, bool singleline)
    {
        if (!this->current->isActive() || this->current->getStreamIndex() >= stream_size)
            return false;

        if (this->anchor_type == AnchorType::START && !this->testStartAnchor(stream, multiline))
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
                if (!this->padding && !singleline && stream[this->current->getStreamIndex()] == '\n')
                    return false;
                break;
            default:
                return false;
        }

        this->next->setStreamIndex(this->current->getStreamIndex() + this->stream_increment);        
        if (this->anchor_type == AnchorType::END && !this->testEndAnchor(stream, stream_size, multiline))
            return false;

        this->next->setActive(true);
        return true;
    }

    bool testFifo(char *stream, int stream_size, Fifo* fifo, bool multiline, bool insensitive)
    {
        if (this->type != TransitionType::Backreference)
            return false;

        if (!this->current->isActive() || this->current->getStreamIndex() >= stream_size)
            return false;

        if (this->anchor_type == AnchorType::START && !this->testStartAnchor(stream, multiline))
            return false;

        this->stream_increment = fifo->checkQueue(stream, this->current->getStreamIndex(), insensitive);
        if (!this->stream_increment)
            return false;
        
        this->next->setStreamIndex(this->current->getStreamIndex() + this->stream_increment);
        if (this->anchor_type == AnchorType::END && !this->testEndAnchor(stream, stream_size, multiline))
            return false;

        this->next->setActive(true);
        return true;
    }
};

class CharacterTransition : public Transition<1> {
    public:
    CharacterTransition(int token, State *current, State *next, AnchorType anchor_type) : Transition<1>(TransitionType::Character, std::array<int, 1>({token}), current, next, 1, anchor_type)
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
    WildcardTransition(bool padding, State *current, State *next, AnchorType anchor_type) : Transition<0>(TransitionType::Wildcard, std::array<int, 0>(), current, next, 1, anchor_type)
    { 
        this->padding = padding;
    }
};

class BackreferenceTransition : public Transition<0> {
    public:
    BackreferenceTransition(State *current, State *next, AnchorType anchor_type) : Transition<0>(TransitionType::Backreference, std::array<int, 0>(), current, next, 0, anchor_type)
    { }
};

<#list automata as automaton>
//kernel for /${automaton.expression}/
int kernel${automaton?counter}(char stream[], int stream_size)
{
    State current[${automaton.states?size}];
    State next[${automaton.states?size}];
    current[${automaton.start_state.id}].setActive(true);

    ${declareFifos(automaton.fifo_ids)}

    <#assign transition_counter = 0>
    <#list automaton.transitions as transition>
      <#if transition.type == "Character">
    CharacterTransition t${transition_counter}(${transition.token}, &current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      <#elseif transition.type == "CharacterClass">
    CharacterClassTransition<${transition.tokens?size}> t${transition_counter}(std::array<int, ${transition.tokens?size}>({${transition.tokens?join(", ")}}), ${transition.negated?c}, &current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      <#elseif transition.type == "Wildcard">
    WildcardTransition t${transition_counter}(${transition.padding?c}, &current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      <#elseif transition.type == "Backreference">
    BackreferenceTransition t${transition_counter}(&current[${transition.source.id}], &next[${transition.target.id}], AnchorType::${transition.anchor_info});
      </#if>
    <#assign transition_counter += 1>
    </#list>

    int transitions_occurred;
    bool in_end_state = false;

    do {
        transitions_occurred = 0;
     
        <#assign transition_counter = 0>
        <#list automaton.transitions as transition>
        <#assign callTest = "">
        <#if transition.type == "Backreference">
          <#assign callTest = "t${transition_counter}.testFifo(stream, stream_size, " +  "&${transition.fifo_to_match}"?no_esc + ", ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c})">
        <#else>
          <#assign callTest = "t${transition_counter}.test(stream, stream_size, ${hasMultilineFlag(automaton)?c}, ${hasCaseInsensitiveFlag(automaton)?c}, ${hasSinglelineFlag(automaton)?c})">
        </#if>
        <#if transition.fifos_info?size == 0>
          transitions_occurred += ${callTest};
        <#else>
          if (${callTest}) 
          {
            transitions_occurred++;
            <#list transition.fifos_info as fifo_info>
            ${fifo_info.fifo.id}.enqueue(stream[t${transition_counter}.getCurrent()->getStreamIndex()], ${fifo_info.clear?c});
            </#list>
          }
        </#if>
        <#assign transition_counter += 1>
        </#list>

        if (transitions_occurred)
        {
            memcpy(current, next, ${automaton.states?size} * sizeof(State));
            memset(next, 0, ${automaton.states?size} * sizeof(State));
            in_end_state = <#list automaton.end_states as end_state>current[${end_state.id}].isActive()<#if !end_state?is_last> || </#if></#list>;
        }
      
    } while (transitions_occurred && !in_end_state);

    return in_end_state;
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
      <#assign result = result + "Fifo ${fifo_id};">
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