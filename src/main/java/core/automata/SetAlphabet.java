package core.automata;

import api.automata.Alphabet;
import api.automata.Symbol;
import org.eclipse.collections.api.set.ImmutableSet;

public class SetAlphabet<S extends Symbol> implements Alphabet<S>
{
    private final ImmutableSet<S> symbolSet;
    private final S epsilon;

    public SetAlphabet(ImmutableSet<S> definition, S epsilonSymbol)
    {
        if (!definition.contains(epsilonSymbol)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        symbolSet = definition;
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
    public boolean contains(S symbol)
    {
        return symbolSet.contains(symbol);
    }

    @Override
    public ImmutableSet<S> toSet()
    {
        return symbolSet;
    }
}
