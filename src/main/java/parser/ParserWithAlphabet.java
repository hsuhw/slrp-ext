package parser;

import automata.Alphabet;

public interface ParserWithAlphabet<T extends Alphabet> extends Parser
{
    T getAlphabet();
}
