package api.automata;

import core.automata.SetAlphabet;
import core.automata.SetAlphabetBuilder;
import org.eclipse.collections.api.set.MutableSetIterable;

public final class Alphabets
{
    private Alphabets()
    {
    }

    public static <S> Alphabet.Builder<S> builder(int symbolNumberEstimate)
    {
        return new SetAlphabetBuilder<>(symbolNumberEstimate);
    }

    public static <S> Alphabet<S> create(MutableSetIterable<S> definition, S epsilonSymbol)
    {
        return new SetAlphabet<>(definition, epsilonSymbol);
    }
}
