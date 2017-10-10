package core.automata;

import api.automata.Alphabet;
import core.util.Assertions;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

public class SetAlphabet<S> implements Alphabet<S>
{
    private final ImmutableSet<S> symbols;
    private final S epsilon;

    public SetAlphabet(MutableSet<S> definition, S epsilon)
    {
        Assertions.argumentNotNull(epsilon);
        if (!definition.contains(epsilon)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.contains(null)) {
            throw new IllegalArgumentException("a null reference found in the definition");
        }

        symbols = definition.toImmutable();
        this.epsilon = epsilon;
    }

    @Override
    public int size()
    {
        return symbols.size();
    }

    @Override
    public S epsilon()
    {
        return epsilon;
    }

    @Override
    public ImmutableSet<S> set()
    {
        return symbols;
    }

    @Override
    public ImmutableSet<S> noEpsilonSet()
    {
        return symbols.newWithout(epsilon);
    }
}
