package api.automata.fsa;

import api.automata.*;
import core.automata.fsa.BasicFSABuilder;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;

public final class FSAs
{
    private FSAs()
    {
    }

    public static <S> BasicFSABuilder<S> builder(int symbolNumberEstimate, S epsilonSymbol, int stateNumberEstimate)
    {
        return new BasicFSABuilder<>(symbolNumberEstimate, epsilonSymbol, stateNumberEstimate);
    }

    public static <S> FSA<S> create(Alphabet<S> alphabet, MutableSet<State> states, MutableSet<State> startStates,
                                    MutableSet<State> acceptStates, DeltaFunction.Builder<S> deltaBuilder)
    {
        return BasicFSABuilder.make(alphabet, states, startStates, acceptStates, deltaBuilder);
    }

    public static <S> FSA<S> withEmptyLanguage(Alphabet<S> alphabet)
    {
        final State state = States.generate();
        final DeltaFunction.Builder<S> deltaBuilder = DeltaFunctions.builder(1, alphabet.getEpsilonSymbol());
        alphabet.getSet().forEach(symbol -> deltaBuilder.addTransition(state, state, symbol));
        final MutableSet<State> stateSingleton = Sets.fixedSize.of(state);

        return create(alphabet, stateSingleton, stateSingleton, Sets.fixedSize.empty(), deltaBuilder);
    }

    public static <S> FSA<S> withSigmaStarLanguage(Alphabet<S> alphabet)
    {
        final State state = States.generate();
        final DeltaFunction.Builder<S> deltaBuilder = DeltaFunctions.builder(1, alphabet.getEpsilonSymbol());
        alphabet.getSet().forEach(symbol -> deltaBuilder.addTransition(state, state, symbol));
        final MutableSet<State> stateSingleton = Sets.fixedSize.of(state);

        return create(alphabet, stateSingleton, stateSingleton, stateSingleton, deltaBuilder);
    }
}
