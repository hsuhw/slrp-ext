package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableState;
import api.automata.State;
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

    public static <T> MutableFSA<MutableState<T>, T> create(Alphabet<T> alphabet, int stateCapacity)
    {
        return new BasicMutableFSA<>(alphabet, stateCapacity);
    }

    public static <S extends MutableState<T>, T> MutableFSA<S, T> ofSameType(MutableFSA<S, T> target, int stateCapacity)
    {
        final MutableFSA<MutableState<T>, T> result = FSAs.create(target.alphabet(), stateCapacity);
        final MutableState<T> dummyStart = result.startState();
        final S realStart = target.newState();
        target.removeState(realStart);
        result.addState(realStart).setAsStart(realStart).removeState(dummyStart);

        @SuppressWarnings("unchecked")
        final MutableFSA<S, T> resultCasted = (MutableFSA<S, T>) result;
        return resultCasted;
    }

    public static <S extends MutableState<T>, T> MutableFSA<S, T> shallowCopy(MutableFSA<S, T> target)
    {
        if (target instanceof BasicMutableFSA<?>) {
            final BasicMutableFSA<T> targetCasted = (BasicMutableFSA<T>) target;
            @SuppressWarnings("unchecked")
            final MutableFSA<S, T> result = (MutableFSA<S, T>) new BasicMutableFSA<>(targetCasted, false);
            return result;
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    public static <S extends MutableState<T>, T> MutableFSA<S, T> deepCopy(MutableFSA<S, T> target)
    {
        if (target instanceof BasicMutableFSA<?>) {
            final BasicMutableFSA<T> targetCasted = (BasicMutableFSA<T>) target;
            @SuppressWarnings("unchecked")
            final MutableFSA<S, T> r = (MutableFSA<S, T>) new BasicMutableFSA<>(targetCasted, true);
            return r;
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    private static <T> FSA<State<T>, T> createAcceptingNone(Alphabet<T> alphabet)
    {
        return FSA.upcast(create(alphabet, 1));
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <T> FSA<State<T>, T> acceptingNone(Alphabet<T> alphabet)
    {
        Pair<SoftReference<Alphabet>, FSA> cache = NONE_CACHE.get(alphabet);
        if (cache != null && cache.getOne().get() != null) {
            @SuppressWarnings("unchecked")
            final FSA<State<T>, T> cachedInstance = cache.getTwo();
            return cachedInstance;
        }

        final FSA<State<T>, T> instance = createAcceptingNone(alphabet);
        NONE_CACHE.put(alphabet, Tuples.pair(new SoftReference<>(alphabet), instance));

        return instance;
    }

    private static <T> FSA<State<T>, T> createAcceptingAll(Alphabet<T> alphabet)
    {
        final MutableFSA<MutableState<T>, T> result = create(alphabet, 1);

        final MutableState<T> state = result.startState();
        alphabet.noEpsilonSet().forEach(symbol -> result.addTransition(state, state, symbol));

        return FSA.upcast(result);
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <T> FSA<State<T>, T> acceptingAll(Alphabet<T> alphabet)
    {
        Pair<SoftReference<Alphabet>, FSA> cache = ALL_CACHE.get(alphabet);
        if (cache != null && cache.getOne().get() != null) {
            @SuppressWarnings("unchecked")
            final FSA<State<T>, T> cachedInstance = cache.getTwo();
            return cachedInstance;
        }

        final FSA<State<T>, T> instance = createAcceptingAll(alphabet);
        ALL_CACHE.put(alphabet, Tuples.pair(new SoftReference<>(alphabet), instance));

        return instance;
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <T> FSA<State<T>, T> acceptingOnly(Alphabet<T> alphabet, ListIterable<T> word)
    {
        return acceptingOnly(alphabet, Sets.immutable.of(word));
    }

    /**
     * Should use {@link ImmutableFSA} instead of just {@link FSA}.
     */
    public static <T> FSA<State<T>, T> acceptingOnly(Alphabet<T> alphabet, SetIterable<ListIterable<T>> words)
    {
        final int stateCapacity = (int) words.sumOfInt(ListIterable::size); // upper bound
        final MutableFSA<MutableState<T>, T> result = create(alphabet, stateCapacity);
        final MutableState<T> startState = result.startState();
        final MutableState<T> acceptState = result.newState();
        result.setAsAccept(acceptState);

        MutableState<T> currState = startState, nextState;
        int lastSymbolPos;
        T symbol;
        for (ListIterable<T> word : words) {
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

        return FSA.upcast(result);
    }
}
