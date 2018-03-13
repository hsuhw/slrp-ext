package api.automata;

import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;

public interface Alphabet<S>
{
    default int size()
    {
        return asSet().size();
    }

    S epsilon();

    default boolean isEpsilon(S symbol)
    {
        return symbol.equals(epsilon());
    }

    default boolean notEpsilon(S symbol)
    {
        return !isEpsilon(symbol);
    }

    SetIterable<S> asSet();

    default SetIterable<S> noEpsilonSet()
    {
        return asSet().difference(Sets.immutable.of(epsilon()));
    }

    default boolean contains(S symbol)
    {
        return asSet().contains(symbol);
    }

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    interface Builder<S>
    {
        S epsilon();

        Builder<S> add(S symbol);

        SetIterable<S> addedSymbols();

        Alphabet<S> build();
    }
}
