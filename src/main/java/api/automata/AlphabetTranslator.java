package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * The alphabet translating class that uses {@link T} symbols to represent
 * the original {@link O} alphabet symbols.
 *
 * @param <O> the original symbol type
 * @param <T> the mapped symbol type
 */
public interface AlphabetTranslator<O, T extends Symbol>
{
    int size();

    T getTargetEpsilonSymbol();

    O getOriginEpsilonSymbol();

    T targetSymbolOf(O symbol);

    O originSymbolOf(T symbol);

    Alphabet<T> getTargetAlphabet();

    ImmutableSet<O> getOriginAlphabet();

    default ImmutableList<T> translate(ImmutableList<O> word)
    {
        return word.collect(this::targetSymbolOf);
    }

    default ImmutableList<O> translateBack(ImmutableList<T> word)
    {
        return word.collect(this::originSymbolOf);
    }
}
