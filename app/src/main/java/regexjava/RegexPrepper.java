package regexjava;

import java.util.Stack;
import java.util.Map;

public enum RegexPrepper {
    INSTANCE();

    private static String toPostfix(String regex)
    {
        String res = "";
        Stack<Character> operators = new Stack<>();
        Map<Character, Integer> precedence = Map.of('|', 0, '.', 1, '?', 2, '*', 2, '+', 2);
    
        for (final char token : regex.toCharArray()) 
        {
            if (token == '.' || token == '|' || token == '*' || token == '?' || token == '+')
            {
                while (!operators.empty() && operators.peek() != '(' && precedence.get(operators.peek()) >= precedence.get(token)) 
                    res += operators.pop();
    
                operators.push(token);
            }
            else if (token == '(' || token == ')') 
            {
                if (token == '(') 
                    operators.push(token);
                else 
                {
                    while (operators.peek() != '(') 
                        res += operators.pop();

                    operators.pop();
                }
            }
            else 
                res += token;
        }
    
        while (!operators.empty())
            res += operators.pop();
    
        return res;
    }

    private static String addExplicitConcat(String regex)
    {
        String res = "";

        for(int i = 0; i < regex.length(); i++)
        {
            final char token = regex.charAt(i);
            res += token;
    
            if (token == '(' || token == '|')
                continue;
    
            if (i + 1 < regex.length())
            {
                final char next_token = regex.charAt(i + 1);
                if (next_token == '*' || next_token == '?' || next_token == '+' || next_token == '|' || next_token == ')')
                    continue;
                
                res += '.';
            }
        }
    
        return res;
    }

    public static String prep(String regex)
    {
        return toPostfix(addExplicitConcat(regex));
    }
}
