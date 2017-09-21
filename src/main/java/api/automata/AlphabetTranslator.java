package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;

/**
 * The alphabet translating class that uses {@link T} symbols to represent
 * the original {@link O} alphabet symbols.
 *
 * @param <O> the original symbol type
 * @param <T> the mapped symbol type
 */
public interface AlphabetTranslator<O, T>
{
    int size();

    T getTargetEpsilonSymbol();

    O getOriginEpsilonSymbol();

    T targetSymbolOf(O symbol);

    O originSymbolOf(T symbol);

    Alphabet<T> getTargetAlphabet();

    Alphabet<O> getOriginAlphabet();

    default ListIterable<T> translate(ImmutableList<O> word)
    {
        return word.collect(this::targetSymbolOf);
    }

    default ListIterable<O> translateBack(ImmutableList<T> word)
    {
        return word.collect(this::originSymbolOf);
    }

    interface Builder<O, T>
    {
        Builder<O, T> define(O origin, T target);

        Builder<O, T> defineEpsilon(O origin, T target);

        AlphabetTranslator<O, T> build();
    }
}
