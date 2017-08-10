package api.parser;

import api.automata.AlphabetTranslator;

public interface ParserWithAlphabet<T, A extends AlphabetTranslator> extends Parser<T>
{
    A getAlphabetMapping();
}
