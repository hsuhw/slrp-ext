package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;

public interface Alphabet<S>
{
    int size();

    S epsilon();

    ImmutableSet<S> set();

    ImmutableSet<S> noEpsilonSet();

    interface Builder<S>
    {
        Builder<S> add(S symbol);

        ImmutableSet<S> addedSymbols();

        Alphabet<S> build();
    }
}
