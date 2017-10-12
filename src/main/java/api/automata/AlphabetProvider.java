package api.automata;

import org.eclipse.collections.api.set.MutableSet;

import static api.automata.Alphabet.Builder;

public interface AlphabetProvider
{
    <S> Builder<S> builder(int sizeEstimate, S epsilon);

    <S> Builder<S> builderBasedOn(Alphabet<S> alphabet);

    <S> Alphabet<S> create(MutableSet<S> definition, S epsilon);
}
