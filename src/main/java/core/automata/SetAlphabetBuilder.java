package core.automata;

import api.automata.Alphabet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.Alphabet.Builder;
import static core.util.Parameters.estimateExtendedSize;

public class SetAlphabetBuilder<S> implements Builder<S>
{
    private final MutableSet<S> symbolSet;
    private S epsilonSymbol;

    public SetAlphabetBuilder(int symbolNumberEstimate)
    {
        symbolSet = UnifiedSet.newSet(symbolNumberEstimate);
    }

    public SetAlphabetBuilder(SetAlphabet<S> alphabet)
    {
        symbolSet = UnifiedSet.newSet(estimateExtendedSize(alphabet.size()));
        symbolSet.addAllIterable(alphabet.getSet());
    }

    @Override
    public Builder<S> add(S symbol)
    {
        symbolSet.add(symbol);

        return this;
    }

    @Override
    public Builder<S> defineEpsilon(S symbol)
    {
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
