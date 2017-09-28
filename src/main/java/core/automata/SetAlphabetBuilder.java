package core.automata;

import api.automata.Alphabet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.Alphabet.Builder;
import static core.util.Parameters.estimateExtendedSize;

public class SetAlphabetBuilder<S> implements Alphabet.Builder<S>
{
    private final MutableSet<S> symbolSet;
    private S epsilonSymbol;

    public SetAlphabetBuilder(int symbolNumberEstimate, S epsilonSymbol)
    {
        symbolSet = UnifiedSet.newSet(symbolNumberEstimate);
        this.epsilonSymbol = epsilonSymbol;
        symbolSet.add(epsilonSymbol);
    }

    public SetAlphabetBuilder(SetAlphabet<S> alphabet)
    {
        symbolSet = UnifiedSet.newSet(estimateExtendedSize(alphabet.size()));
        symbolSet.addAllIterable(alphabet.getSet());
        epsilonSymbol = alphabet.getEpsilonSymbol();
    }

    @Override
    public Builder<S> add(S symbol)
    {
        symbolSet.add(symbol);

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
