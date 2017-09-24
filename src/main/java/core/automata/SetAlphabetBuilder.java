package core.automata;

import api.automata.Alphabet;
import core.util.Assertions;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.Alphabet.Builder;

public class SetAlphabetBuilder<S> implements Builder<S>
{
    private final MutableSet<S> symbolSet;
    private S epsilonSymbol;

    public SetAlphabetBuilder(int symbolNumberEstimate)
    {
        symbolSet = UnifiedSet.newSet(symbolNumberEstimate);
    }

    @Override
    public Builder<S> add(S symbol)
    {
        Assertions.argumentNotNull(symbol);

        symbolSet.add(symbol);

        return this;
    }

    @Override
    public Builder<S> defineEpsilon(S symbol)
    {
        Assertions.argumentNotNull(symbol);

        symbolSet.add(symbol);
        epsilonSymbol = symbol;

        return this;
    }

    @Override
    public SetIterable<S> getAddedSymbols()
    {
        return symbolSet.toImmutable(); // defense required
    }

    @Override
    public Alphabet<S> build()
    {
        return new SetAlphabet<>(symbolSet, epsilonSymbol);
    }
}
