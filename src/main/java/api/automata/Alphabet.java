package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;

public interface Alphabet<S extends Symbol>
{
    int size();

    S getEpsilonSymbol();

    boolean contains(S symbol);

    ImmutableSet<S> toSet();
}
