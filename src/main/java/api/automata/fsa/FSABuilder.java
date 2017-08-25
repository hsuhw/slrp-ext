package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.AutomatonBuilder;
import api.automata.Symbol;

public interface FSABuilder<S extends Symbol> extends AutomatonBuilder<S>
{
    Alphabet<S> getCurrentAlphabet();

    void addSymbol(S symbol);

    @Override
    FSA<S> build();

    FSA<S> build(Alphabet<S> alphabetOverride);
}
