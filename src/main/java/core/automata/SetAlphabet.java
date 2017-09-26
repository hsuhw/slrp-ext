package core.automata;

import api.automata.Alphabet;
import core.util.Assertions;
import org.eclipse.collections.api.set.ImmutableSetIterable;
import org.eclipse.collections.api.set.MutableSetIterable;
import org.eclipse.collections.api.set.SetIterable;

public class SetAlphabet<S> implements Alphabet<S>
{
    private final ImmutableSetIterable<S> symbolSet;
    private final S epsilon;

    public SetAlphabet(MutableSetIterable<S> definition, S epsilonSymbol)
    {
        Assertions.argumentNotNull(epsilonSymbol);
        if (!definition.contains(epsilonSymbol)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.contains(null)) {
            throw new IllegalArgumentException("a null reference found in the definition");
        }

        symbolSet = (ImmutableSetIterable<S>) definition.toImmutable();
        epsilon = epsilonSymbol;
    }

    @Override
    public int size()
    {
        return symbolSet.size();
    }

    @Override
    public S getEpsilonSymbol()
    {
        return epsilon;
    }

    @Override
    public SetIterable<S> getSet()
    {
        return symbolSet;
    }
}
