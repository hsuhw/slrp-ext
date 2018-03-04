package api.automata;

import common.util.Assert;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * The alphabet mapping class that uses {@link T} symbols to represent
 * the original {@link S} alphabet symbols.
 *
 * @param <S> the original symbol type
 * @param <T> the mapped symbol type
 */
public interface AlphabetEncoder<S, T>
{
    int size();

    default T encodedEpsilon()
    {
        return encode(originEpsilon());
    }

    Alphabet<T> encodedAlphabet();

    S originEpsilon();

    Alphabet<S> originAlphabet();

    T encode(S symbol);

    default ImmutableList<T> encode(ImmutableList<S> word)
    {
        word.forEach(Assert::argumentNotNull);

        return word.collect(this::encode);
    }

    S decode(T symbol);

    default ImmutableList<S> decode(ImmutableList<T> word)
    {
        word.forEach(Assert::argumentNotNull);

        return word.collect(this::decode);
    }

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);
}
