package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableState;
import core.automata.fsa.BasicMutableFSA;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;

import static common.util.Constants.NO_IMPLEMENTATION_FOUND;

public final class FSAs
{
    private static final WeakHashMap<Alphabet, Pair<SoftReference<Alphabet>, FSA>> NONE_CACHE = new WeakHashMap<>();
    private static final WeakHashMap<Alphabet, Pair<SoftReference<Alphabet>, FSA>> ALL_CACHE = new WeakHashMap<>();

    private FSAs()
    {
    }

    public static <S> MutableFSA<S> create(Alphabet<S> alphabet, int stateCapacity)
    {
        return new BasicMutableFSA<>(alphabet, stateCapacity);
    }

    public static <S> MutableFSA<S> shallowCopy(MutableFSA<S> target)
    {
        if (target instanceof BasicMutableFSA<?>) {
            return new BasicMutableFSA<>((BasicMutableFSA<S>) target, false);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    public static <S> MutableFSA<S> deepCopy(MutableFSA<S> target)
    {
        if (target instanceof BasicMutableFSA<?>) {
            return new BasicMutableFSA<>((BasicMutableFSA<S>) target, true);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    private static <S> FSA<S> createAcceptingNone(Alphabet<S> alphabet)
    {
        return create(alphabet, 1);
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <S> FSA<S> acceptingNone(Alphabet<S> alphabet)
    {
        Pair<SoftReference<Alphabet>, FSA> cache = NONE_CACHE.get(alphabet);
        if (cache != null && cache.getOne().get() != null) {
            @SuppressWarnings("unchecked")
            final FSA<S> cachedResult = cache.getTwo();
            return cachedResult;
        }

        final FSA<S> result = createAcceptingNone(alphabet);
        NONE_CACHE.put(alphabet, Tuples.pair(new SoftReference<>(alphabet), result));

        return result;
    }

    private static <S> FSA<S> createAcceptingAll(Alphabet<S> alphabet)
    {
        final MutableFSA<S> result = create(alphabet, 1);

        final MutableState<S> state = (MutableState<S>) result.startState();
        alphabet.noEpsilonSet().forEach(symbol -> result.addTransition(state, state, symbol));

        return result;
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <S> FSA<S> acceptingAll(Alphabet<S> alphabet)
    {
        Pair<SoftReference<Alphabet>, FSA> cache = ALL_CACHE.get(alphabet);
        if (cache != null && cache.getOne().get() != null) {
            @SuppressWarnings("unchecked")
            final FSA<S> cachedResult = cache.getTwo();
            return cachedResult;
        }

        final FSA<S> result = createAcceptingAll(alphabet);
        ALL_CACHE.put(alphabet, Tuples.pair(new SoftReference<>(alphabet), result));

        return result;
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <S> FSA<S> acceptingOnly(Alphabet<S> alphabet, ListIterable<S> word)
    {
        return acceptingOnly(alphabet, Sets.immutable.of(word));
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <S> FSA<S> acceptingOnly(Alphabet<S> alphabet, SetIterable<ListIterable<S>> words)
    {
        final int stateCapacity = (int) words.sumOfInt(ListIterable::size); // upper bound
        final MutableFSA<S> result = create(alphabet, stateCapacity);
        final MutableState<S> startState = (MutableState<S>) result.startState();
        final MutableState<S> acceptState = result.newState();
        result.setAsAccept(acceptState);

        MutableState<S> currState = startState, nextState;
        int lastSymbolPos;
        S symbol;
        for (ListIterable<S> word : words) {
            if (word.isEmpty()) {
                result.addEpsilonTransition(currState, acceptState);
                continue;
            }
            for (int i = 0; i < (lastSymbolPos = word.size() - 1); i++) {
                if (!(symbol = word.get(i)).equals(alphabet.epsilon())) {
                    nextState = result.newState();
                    result.addTransition(currState, nextState, symbol);
                    currState = nextState;
                }
            }
            result.addTransition(currState, acceptState, word.get(lastSymbolPos));
            currState = startState;
        }

        return result;
    }
}
