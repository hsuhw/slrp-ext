package api.automata;

import org.eclipse.collections.api.set.SetIterable;

public interface Alphabet<S>
{
    int size();

    S getEpsilonSymbol();

    SetIterable<S> getSet();

    interface Builder<S>
    {
        Builder<S> add(S symbol);

        Builder<S> defineEpsilon(S symbol);

        SetIterable<S> getAddedSymbols();

        Alphabet<S> build();
    }
}
