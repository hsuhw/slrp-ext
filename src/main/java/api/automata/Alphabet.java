package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

public interface Alphabet<S>
{
    int size();

    S getEpsilonSymbol();

    ImmutableSet<S> getSet();

    ImmutableSet<S> getNoEpsilonSet();

    interface Builder<S>
    {
        Builder<S> add(S symbol);

        SetIterable<S> getAddedSymbols();

        Alphabet<S> build();
    }
}
