package api.automata;

import core.automata.SetAlphabet;
import core.automata.SetAlphabetBuilder;
import org.eclipse.collections.api.list.ListIterable;
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

    public static <S, T> Alphabet<Pair<S, T>> product(Alphabet<S> one, Alphabet<T> two)
    {
        final SetIterable<S> symbols1 = one.noEpsilonSet();
        final Set<S> symbolSet1 = symbols1 instanceof ImmutableSet<?>
                                  ? ((ImmutableSet<S>) symbols1).castToSet()
                                  : (MutableSet<S>) symbols1;
        final SetIterable<T> symbols2 = two.noEpsilonSet();
        final Set<T> symbolSet2 = symbols2 instanceof ImmutableSet<?>
                                  ? ((ImmutableSet<T>) symbols2).castToSet()
                                  : (MutableSet<T>) symbols2;
        final Pair<S, T> epsilon = Tuples.pair(one.epsilon(), two.epsilon());
        final MutableSet<Pair<S, T>> product = Sets.cartesianProduct(symbolSet1, symbolSet2, Tuples::pair).toSet();
        product.add(epsilon);

        return create(product, epsilon);
    }

    public static <S, T> ListIterable<Pair<S, T>> pairWord(ListIterable<S> one, ListIterable<T> two)
    {
        if (one.size() != two.size()) {
            throw new IllegalArgumentException("size-unmatched word pair given");
        }

        return one.collectWithIndex((symbol, index) -> Tuples.pair(symbol, two.get(index)));
    }
}
