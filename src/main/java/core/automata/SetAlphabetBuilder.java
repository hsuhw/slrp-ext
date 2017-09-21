package core.automata;

import api.automata.Alphabet;
import core.util.Assertions;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public class SetAlphabetBuilder<S> implements Alphabet.Builder<S>
{
    private final MutableSet<S> symbolSet;
    private S epsilonSymbol;

    public SetAlphabetBuilder(int symbolNumberEstimate)
    {
        symbolSet = UnifiedSet.newSet(symbolNumberEstimate);
    }

    @Override
    public Alphabet.Builder<S> add(S symbol)
    {
        Assertions.argumentNotNull(symbol);

        symbolSet.add(symbol);

        return this;
    }

    @Override
    public Alphabet.Builder<S> defineEpsilon(S symbol)
    {
        Assertions.argumentNotNull(symbol);

        symbolSet.add(symbol);
        epsilonSymbol = symbol;

        return this;
    }

    @Override
    public Alphabet<S> build()
    {
        return new SetAlphabet<>(symbolSet, epsilonSymbol);
    }
}
