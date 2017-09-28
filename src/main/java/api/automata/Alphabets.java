package api.automata;

import core.automata.SetAlphabet;
import core.automata.SetAlphabetBuilder;
import org.eclipse.collections.api.set.MutableSet;

import static api.automata.Alphabet.Builder;

public final class Alphabets
{
    private Alphabets()
    {
    }

    public static <S> Builder<S> builder(int symbolNumberEstimate)
    {
        return new SetAlphabetBuilder<>(symbolNumberEstimate);
    }

    public static <S> Builder<S> builderBasedOn(Alphabet<S> alphabet)
    {
        return new SetAlphabetBuilder<>((SetAlphabet<S>) alphabet);
    }

    public static <S> Alphabet<S> create(MutableSet<S> definition, S epsilonSymbol)
    {
        return new SetAlphabet<>(definition, epsilonSymbol);
    }
}