package api.automata;

import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;

/**
 * The alphabet mapping class that uses {@code int} values to represent
 * the original {@link S} alphabet symbols.
 *
 * @param <S> the original symbol type
 */
public interface AlphabetIntEncoder<S>
{
    int INT_EPSILON = 0;

    int size();

    default int encodedEpsilon()
    {
        return INT_EPSILON;
    }

    IntSet encodedAlphabet();

    default S originEpsilon()
    {
        return decode(INT_EPSILON);
    }

    Alphabet<S> originAlphabet();

    int encode(S symbol);

    default ImmutableIntList encode(ImmutableList<S> word)
    {
        word.forEach(Assertions::argumentNotNull);

        return word.collectInt(this::encode);
    }

    S decode(int symbol);

    default ListIterable<S> decode(ImmutableIntList word)
    {
        return word.collect(this::decode);
    }

    default ListIterable<S> decode(int... word)
    {
        return decode(IntLists.immutable.of(word));
    }

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    interface Provider
    {
        <S> AlphabetIntEncoder<S> create(MutableList<S> definition, S epsilon);

        <S> AlphabetIntEncoder<S> create(Alphabet<S> alphabet);
    }
}
