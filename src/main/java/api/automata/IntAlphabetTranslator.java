package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.primitive.IntSet;
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

    IntSet getIntAlphabet();

    SetIterable<S> getOriginAlphabet();

    default ImmutableIntList translate(ImmutableList<S> word)
    {
        return word.collectInt(this::intSymbolOf);
    }

    default ListIterable<S> translateBack(ImmutableIntList word)
    {
        return word.collect(this::originSymbolOf);
    }

    default ListIterable<S> translateBack(int... word)
    {
        return translateBack(IntLists.immutable.of(word));
    }
}
