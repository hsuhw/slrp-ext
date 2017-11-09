package api.automata.fsa;

import api.automata.Alphabet;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.lang.ref.SoftReference;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static api.automata.AutomatonManipulator.Finalizer;
import static api.automata.fsa.FSA.Builder;
import static api.automata.fsa.FSA.Provider;

public final class FSAs
{
    private static final WeakHashMap<Alphabet, Pair<SoftReference<Alphabet>, FSA>> NONE_FSA = new WeakHashMap<>();
    private static final WeakHashMap<Alphabet, Pair<SoftReference<Alphabet>, FSA>> ALL_FSA = new WeakHashMap<>();

    private FSAs()
    {
    }

    public static <S> Builder<S> builder(int stateCapacity, int symbolCapacity, S epsilonSymbol)
    {
        return Singleton.INSTANCE.builder(stateCapacity, symbolCapacity, epsilonSymbol);
    }

    public static <S> Builder<S> builderOn(FSA<S> fsa)
    {
        return Singleton.INSTANCE.builderOn(fsa);
    }

    public static <S> FSA<S> thatAcceptsNone(Alphabet<S> alphabet)
    {
        Pair<SoftReference<Alphabet>, FSA> record = NONE_FSA.get(alphabet);
        if (record != null && record.getOne().get() != null) {
            @SuppressWarnings("unchecked")
            final FSA<S> acceptingNone = record.getTwo();
            return acceptingNone;
        }

        final FSA<S> acceptingNone = Singleton.INSTANCE.thatAcceptsNone(alphabet);
        NONE_FSA.put(alphabet, Tuples.pair(new SoftReference<>(alphabet), acceptingNone));

        return acceptingNone;
    }

    public static <S> FSA<S> thatAcceptsAll(Alphabet<S> alphabet)
    {
        Pair<SoftReference<Alphabet>, FSA> record = ALL_FSA.get(alphabet);
        if (record != null && record.getOne().get() != null) {
            @SuppressWarnings("unchecked")
            final FSA<S> acceptingAll = record.getTwo();
            return acceptingAll;
        }

        final FSA<S> acceptingAll = Singleton.INSTANCE.thatAcceptsAll(alphabet);
        ALL_FSA.put(alphabet, Tuples.pair(new SoftReference<>(alphabet), acceptingAll));

        return acceptingAll;
    }

    public static <S> FSA<S> thatAcceptsOnly(Alphabet<S> alphabet, ImmutableList<S> word)
    {
        return Singleton.INSTANCE.thatAcceptsOnly(alphabet, word);
    }

    public static <S> FSA<S> thatAcceptsOnly(Alphabet<S> alphabet, RichIterable<ImmutableList<S>> words)
    {
        return Singleton.INSTANCE.thatAcceptsOnly(alphabet, words);
    }

    public static FSAManipulator manipulator()
    {
        return Singleton.INSTANCE.manipulator();
    }

    public static <S> FSA<S> trimUnreachableStates(FSA<S> target)
    {
        return manipulator().trimUnreachableStates(target);
    }

    public static <S> FSA<S> trimDeadEndStates(FSA<S> target)
    {
        return manipulator().trimDeadEndStates(target);
    }

    public static <S> FSA<S> trimDanglingStates(FSA<S> target)
    {
        return manipulator().trimDanglingStates(target);
    }

    public static <S, R> FSA<R> project(FSA<S> target, Alphabet<R> alphabet, Function<S, R> projector)
    {
        return manipulator().project(target, alphabet, projector);
    }

    public static <S, T, R> FSA<R> product(FSA<S> one, FSA<T> two, Alphabet<R> alphabet,
                                           BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
    {
        return manipulator().product(one, two, alphabet, transitionDecider, finalizer);
    }

    public static <S> FSA<S> determinize(FSA<S> target)
    {
        return manipulator().determinize(target);
    }

    public static <S> FSA<S> complete(FSA<S> target)
    {
        return manipulator().complete(target);
    }

    public static <S> FSA<S> minimize(FSA<S> target)
    {
        return manipulator().minimize(target);
    }

    public static <S> FSA<S> complement(FSA<S> target)
    {
        return manipulator().complement(target);
    }

    public static <S> FSA<S> intersect(FSA<S> one, FSA<S> two)
    {
        return manipulator().intersect(one, two);
    }

    public static <S> FSA<S> union(FSA<S> one, FSA<S> two)
    {
        return manipulator().union(one, two);
    }

    public static <S> LanguageSubsetChecker.Result<S> checkSubset(FSA<S> subsumer, FSA<S> includer)
    {
        return manipulator().checkSubset(subsumer, includer);
    }

    private static final class Singleton
    {
        private static final Provider INSTANCE;

        static {
            ServiceLoader<Provider> loader = ServiceLoader.load(Provider.class);
            INSTANCE = loader.stream().reduce((__, latter) -> latter) // get the last provider in classpath
                             .orElseThrow(IllegalStateException::new).get();
        }
    }
}
