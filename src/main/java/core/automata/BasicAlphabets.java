package core.automata;

import api.automata.Alphabet;
import org.eclipse.collections.api.set.MutableSet;

import static api.automata.Alphabet.Builder;

public final class BasicAlphabets implements Alphabet.Provider
{
    public <S> Builder<S> builder(int sizeEstimate, S epsilon)
    {
        return new SetAlphabetBuilder<>(sizeEstimate, epsilon);
    }

    public <S> Builder<S> builderOn(Alphabet<S> alphabet)
    {
        return new SetAlphabetBuilder<>((SetAlphabet<S>) alphabet);
    }

    public <S> Alphabet<S> create(MutableSet<S> definition, S epsilon)
    {
        return new SetAlphabet<>(definition, epsilon);
    }
}
