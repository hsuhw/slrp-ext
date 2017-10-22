package api.automata.fsa;

import api.automata.Alphabet;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;

import java.util.ServiceLoader;

import static api.automata.fsa.FSA.Builder;
import static api.automata.fsa.FSA.Provider;

public final class FSAs
{
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
        return Singleton.INSTANCE.thatAcceptsNone(alphabet);
    }

    public static <S> FSA<S> thatAcceptsAll(Alphabet<S> alphabet)
    {
        return Singleton.INSTANCE.thatAcceptsAll(alphabet);
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
