package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

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

    interface Provider
    {
        <S> Builder<S> builder(int sizeEstimate, S epsilon);

        <S> Builder<S> builderOn(Alphabet<S> alphabet);

        <S> Alphabet<S> create(MutableSet<S> definition, S epsilon);
    }
}
