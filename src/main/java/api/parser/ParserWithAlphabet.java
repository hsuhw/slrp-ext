package api.parser;

import api.automata.AlphabetTranslator;

public interface ParserWithAlphabet<T, A extends AlphabetTranslator> extends Parser<T>
{
    enum SymbolCollectingPolicy
    {
        PREDEFINED, AGGREGATE, SEPARATE
    }

    A getAlphabetMapping();
}
