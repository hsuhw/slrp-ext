package api.automata;

import core.automata.SetAlphabet;
import core.automata.SetAlphabetBuilder;
import org.eclipse.collections.api.set.MutableSetIterable;

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

    public static <S> Builder<S> builderOn(Alphabet<S> other)
    {
        return new SetAlphabetBuilder<>((SetAlphabet<S>) other);
    }

    public static <S> Alphabet<S> create(MutableSetIterable<S> definition, S epsilonSymbol)
    {
        return new SetAlphabet<>(definition, epsilonSymbol);
    }
}
