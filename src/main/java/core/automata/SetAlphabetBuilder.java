package core.automata;

import api.automata.Alphabet;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.Alphabet.Builder;
import static core.util.Parameters.estimateExtendedSize;

public class SetAlphabetBuilder<S> implements Alphabet.Builder<S>
{
    private final MutableSet<S> symbols;
    private final S epsilon;

    public SetAlphabetBuilder(int sizeEstimate, S epsilon)
    {
        symbols = UnifiedSet.newSet(sizeEstimate);
        symbols.add(epsilon);
        this.epsilon = epsilon;
    }

    public SetAlphabetBuilder(SetAlphabet<S> alphabet)
    {
        symbols = UnifiedSet.newSet(estimateExtendedSize(alphabet.size()));
        symbols.addAllIterable(alphabet.asSet());
        epsilon = alphabet.epsilon();
    }

    @Override
    public S epsilon()
    {
        return epsilon;
    }

    @Override
    public Builder<S> add(S symbol)
    {
        symbols.add(symbol);

        return this;
    }

    @Override
    public ImmutableSet<S> addedSymbols()
    {
        return symbols.toImmutable(); // defense required
    }

    @Override
    public SetAlphabet<S> build()
    {
        return new SetAlphabet<>(symbols, epsilon);
    }
}
