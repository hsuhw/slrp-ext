package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;

/**
 * The alphabet translating class that uses {@code int} values to represent
 * the original {@link S} alphabet symbols.
 *
 * @param <S> the original symbol type
 */
public interface IntAlphabetTranslator<S>
{
    int EPSILON = 0;

    int size();

    default int getTargetEpsilonSymbol()
    {
        return EPSILON;
    }

    default S getOriginEpsilonSymbol()
    {
        return originSymbolOf(EPSILON);
    }

    int intSymbolOf(S symbol);

    S originSymbolOf(int symbol);

    ImmutableIntSet getTargetAlphabet();

    ImmutableSet<S> getOriginAlphabet();

    default ImmutableIntList translate(ImmutableList<S> word)
    {
        return word.collectInt(this::intSymbolOf);
    }

    default ImmutableList<S> translateBack(ImmutableIntList word)
    {
        return word.collect(this::originSymbolOf);
    }
}
