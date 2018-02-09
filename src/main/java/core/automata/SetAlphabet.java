package core.automata;

import api.automata.Alphabet;
import common.util.Assert;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

public class SetAlphabet<S> implements Alphabet<S>
{
    private final ImmutableSet<S> symbols;
    private final S epsilon;
    private int hashCode = -1;

    public SetAlphabet(ImmutableSet<S> definition, S epsilon)
    {
        Assert.argumentNotNull(epsilon);
        if (!definition.contains(epsilon)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.contains(null)) {
            throw new IllegalArgumentException("null found in the definition");
        }

        symbols = definition;
        this.epsilon = epsilon;
    }

    public SetAlphabet(MutableSet<S> definition, S epsilon)
    {
        this(definition.toImmutable(), epsilon);
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
    public ImmutableSet<S> asSet()
    {
        return symbols;
    }

    @Override
    public ImmutableSet<S> noEpsilonSet()
    {
        return symbols.newWithout(epsilon);
    }

    @Override
    public int hashCode()
    {
        if (hashCode == -1) {
            final int prime = 71;
            int result = 1;

            result = prime * result + symbols.hashCode();
            result = prime * result + epsilon.hashCode();

            hashCode = result;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof SetAlphabet<?>) {
            try {
                @SuppressWarnings("unchecked")
                final SetAlphabet<S> other = (SetAlphabet<S>) obj;
                return other.symbols.equals(this.symbols) && other.epsilon.equals(this.epsilon);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;
    }
}
