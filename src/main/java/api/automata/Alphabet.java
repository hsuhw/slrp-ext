package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Set;

public interface Alphabet<S>
{
    int size();

    S epsilon();

    ImmutableSet<S> set();

    ImmutableSet<S> noEpsilonSet();

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    interface Builder<S>
    {
        S epsilon();

        Builder<S> add(S symbol);

        ImmutableSet<S> addedSymbols();

        Alphabet<S> build();
    }

    interface Provider
    {
        <S> Builder<S> builder(int sizeEstimate, S epsilon);

        <S> Builder<S> builder(Alphabet<S> base);

        <S> Alphabet<S> create(ImmutableSet<S> definition, S epsilon);

        default <S> Alphabet<S> create(MutableSet<S> definition, S epsilon)
        {
            return create(definition.toImmutable(), epsilon);
        }

        default <S> Alphabet<Twin<S>> product(Alphabet<S> alphabet)
        {
            final Set<S> symbols = alphabet.noEpsilonSet().castToSet();
            final Twin<S> epsilon = Tuples.twin(alphabet.epsilon(), alphabet.epsilon());
            final MutableSet<Twin<S>> product = Sets.cartesianProduct(symbols, symbols, Tuples::twin).toSet();
            product.add(epsilon);

            return create(product, epsilon);
        }

        default <S> ImmutableList<Twin<S>> twinWord(ImmutableList<S> one, ImmutableList<S> two)
        {
            if (one.size() != two.size()) {
                throw new IllegalArgumentException("size-unmatched word pair given");
            }

            return IntInterval.zeroTo(one.size() - 1).collect(i -> Tuples.twin(one.get(i), two.get(i)));
        }
    }
}
