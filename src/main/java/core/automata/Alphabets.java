package core.automata;

import api.automata.Alphabet;
import api.automata.Symbol;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

public final class Alphabets
{
    public static <S extends Symbol> Alphabet<S> createOne(ImmutableSet<S> definition, S epsilonSymbol)
    {
        return new SetAlphabet<>(definition, epsilonSymbol);
    }

    public static <S extends Symbol> Alphabet<S> createOne(MutableSet<S> definition, S epsilonSymbol)
    {
        return new SetAlphabet<>(definition.toImmutable(), epsilonSymbol);
    }
}
