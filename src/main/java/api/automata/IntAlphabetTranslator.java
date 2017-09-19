package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;

/**
 * The alphabet translating class that uses {@code int} values to represent
 * the original {@link S} alphabet symbols.
 *
 * @param <S> the original symbol type
 */
public interface IntAlphabetTranslator<S>
{
    int INT_EPSILON = 0;

    int size();

    default int getIntEpsilonSymbol()
    {
        return INT_EPSILON;
    }

    default S getOriginEpsilonSymbol()
    {
        return originSymbolOf(INT_EPSILON);
    }

    int intSymbolOf(S symbol);

    S originSymbolOf(int symbol);

    ImmutableIntSet getIntAlphabet();

    ImmutableSet<S> getOriginAlphabet();

    default ImmutableIntList translate(ImmutableList<S> word)
    {
        return word.collectInt(this::intSymbolOf);
    }

    default ImmutableList<S> translateBack(ImmutableIntList word)
    {
        return word.collect(this::originSymbolOf);
    }

    default ImmutableList<S> translateBack(int... word)
    {
        return translateBack(IntLists.immutable.of(word));
    }
}
