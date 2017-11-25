package core.automata;

import api.automata.Alphabet;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

import static api.automata.Alphabet.Builder;

public final class BasicAlphabetProvider implements Alphabet.Provider
{
    public <S> Builder<S> builder(int sizeEstimate, S epsilon)
    {
        return new SetAlphabetBuilder<>(sizeEstimate, epsilon);
    }

    public <S> Builder<S> builder(Alphabet<S> base)
    {
        return new SetAlphabetBuilder<>((SetAlphabet<S>) base);
    }

    public <S> Alphabet<S> create(ImmutableSet<S> definition, S epsilon)
    {
        return new SetAlphabet<>(definition, epsilon);
    }
}
