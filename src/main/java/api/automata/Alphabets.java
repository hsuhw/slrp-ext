package api.automata;

import core.automata.SetAlphabet;
import core.automata.SetAlphabetBuilder;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Set;

import static api.automata.Alphabet.Builder;
import static common.util.Constants.NO_IMPLEMENTATION_FOUND;

public final class Alphabets
{
    private Alphabets()
    {
    }

    public static <S> Builder<S> builder(int capacity, S epsilon)
    {
        return new SetAlphabetBuilder<>(capacity, epsilon);
    }

    public static <S> Builder<S> builder(Alphabet<S> base)
    {
        if (base instanceof SetAlphabet<?>) {
            return new SetAlphabetBuilder<>((SetAlphabet<S>) base);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    public static <S> Alphabet<S> create(ImmutableSet<S> definition, S epsilon)
    {
        return new SetAlphabet<>(definition, epsilon);
    }

    public static <S> Alphabet<S> create(MutableSet<S> definition, S epsilon)
    {
        return new SetAlphabet<>(definition, epsilon);
    }

    public static <S> Alphabet<Pair<S, S>> product(Alphabet<S> alphabet)
    {
        final SetIterable<S> symbols = alphabet.noEpsilonSet();
        final Set<S> symbolSet = symbols instanceof ImmutableSet<?>
                                 ? ((ImmutableSet<S>) symbols).castToSet()
                                 : (MutableSet<S>) symbols;
        final Twin<S> epsilon = Tuples.twin(alphabet.epsilon(), alphabet.epsilon());
        final MutableSet<Pair<S, S>> product = Sets.cartesianProduct(symbolSet, symbolSet, Tuples::pair).toSet();
        product.add(epsilon);

        return create(product, epsilon);
    }

    public static <S, T> ImmutableList<Pair<S, T>> pairWord(ImmutableList<S> one, ImmutableList<T> two)
    {
        if (one.size() != two.size()) {
            throw new IllegalArgumentException("size-unmatched word pair given");
        }

        return one.collectWithIndex((symbol, index) -> Tuples.pair(symbol, two.get(index)));
    }
}
