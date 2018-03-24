package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableAutomaton;
import api.automata.MutableState;
import core.automata.AbstractMutableAutomaton;
import core.automata.fsa.BasicMutableFSA;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;

import static common.util.Constants.NO_IMPLEMENTATION_FOUND;

public final class FSAs
{
    // TODO: verify that the caching is working correctly
    private static final WeakHashMap<Alphabet, SoftReference<FSA>> NONE_FSA_CACHE;
    private static final WeakHashMap<Alphabet, SoftReference<FSA>> ALL_FSA_CACHE;
    private static final WeakHashMap<Alphabet, SoftReference<MutableIntObjectMap<FSA>>> FIXED_LENGTH_FSA_CACHE;

    static {
        NONE_FSA_CACHE = new WeakHashMap<>();
        ALL_FSA_CACHE = new WeakHashMap<>();
        FIXED_LENGTH_FSA_CACHE = new WeakHashMap<>();
    }

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

    public static <S> FSA<S> castFrom(MutableAutomaton<S> target)
    {
        if (target instanceof AbstractMutableAutomaton<?>) {
            return new BasicMutableFSA<>((AbstractMutableAutomaton<S>) target);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    private static <S> FSA<S> createAcceptingNone(Alphabet<S> alphabet)
    {
        return create(alphabet, 1);
    }

    public static <S> FSA<S> acceptingNone(Alphabet<S> alphabet)
    {
        final var cache = NONE_FSA_CACHE.get(alphabet);
        FSA cachedItem;
        if (cache != null && (cachedItem = cache.get()) != null) {
            @SuppressWarnings("unchecked")
            final FSA<S> result = cachedItem;
            return result;
        }

        final var result = createAcceptingNone(alphabet);
        NONE_FSA_CACHE.put(alphabet, new SoftReference<>(result));

        return result;
    }

    private static <S> FSA<S> createAcceptingAll(Alphabet<S> alphabet)
    {
        final MutableFSA<S> result = create(alphabet, 1);

        final MutableState<S> startState = result.startState();
        alphabet.noEpsilonSet().forEach(symbol -> result.addTransition(startState, startState, symbol));
        result.setAsAccept(startState);

        return result;
    }

    public static <S> FSA<S> acceptingAll(Alphabet<S> alphabet)
    {
        final var cache = ALL_FSA_CACHE.get(alphabet);
        FSA cachedItem;
        if (cache != null && (cachedItem = cache.get()) != null) {
            @SuppressWarnings("unchecked")
            final FSA<S> result = cachedItem;
            return result;
        }

        final var result = createAcceptingAll(alphabet);
        ALL_FSA_CACHE.put(alphabet, new SoftReference<>(result));

        return result;
    }

    private static <S> FSA<S> createAcceptingAllOnLength(Alphabet<S> alphabet, int length)
    {
        final var result = create(alphabet, length + 1);

        var currState = result.startState();
        for (var i = 0; i < length; i++) {
            final var nextState = result.newState();
            for (var symbol : alphabet.noEpsilonSet()) {
                result.addTransition(currState, nextState, symbol);
            }
            currState = nextState;
        }
        result.setAsAccept(currState);

        return result;
    }

    public static <S> FSA<S> acceptingAllOnLength(Alphabet<S> alphabet, int length)
    {
        final var cache = FIXED_LENGTH_FSA_CACHE.get(alphabet);
        MutableIntObjectMap<FSA> cachedItems;
        if (cache != null && (cachedItems = cache.get()) != null) {
            if (cachedItems.containsKey(length)) {
                @SuppressWarnings("unchecked")
                final FSA<S> result = cachedItems.get(length);
                return result;
            }

            final var result = createAcceptingAllOnLength(alphabet, length);
            cachedItems.put(length, result);
            return result;
        }

        final var result = createAcceptingAllOnLength(alphabet, length);
        final var lengthCache = new IntObjectHashMap<FSA>();
        lengthCache.put(length, result);
        FIXED_LENGTH_FSA_CACHE.put(alphabet, new SoftReference<>(lengthCache));

        return result;
    }

    public static <S> FSA<S> acceptingOnly(Alphabet<S> alphabet, ListIterable<S> word)
    {
        return acceptingOnly(alphabet, Sets.immutable.of(word));
    }

    public static <S> FSA<S> acceptingOnly(Alphabet<S> alphabet, RichIterable<ListIterable<S>> words)
    {
        final int stateCapacity = (int) words.sumOfInt(ListIterable::size); // upper bound
        final MutableFSA<S> result = create(alphabet, stateCapacity);
        final MutableState<S> startState = result.startState();
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
