package core.automata;

import api.automata.Alphabet;
import core.util.Assertions;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

public class SetAlphabet<S> implements Alphabet<S>
{
    private final ImmutableSet<S> symbolSet;
    private final S epsilon;

    public SetAlphabet(MutableSet<S> definition, S epsilonSymbol)
    {
        Assertions.argumentNotNull(epsilonSymbol);
        if (!definition.contains(epsilonSymbol)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.contains(null)) {
            throw new IllegalArgumentException("a null reference found in the definition");
        }

        symbolSet = definition.toImmutable();
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
    public ImmutableSet<S> getSet()
    {
        return symbolSet;
    }

    @Override
    public ImmutableSet<S> getNoEpsilonSet()
    {
        return symbolSet.newWithout(epsilon);
    }
}
