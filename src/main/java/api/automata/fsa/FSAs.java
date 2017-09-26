package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.State;
import api.automata.States;
import core.automata.fsa.BasicFSABuilder;
import core.automata.fsa.MapMapDFSA;
import core.automata.fsa.MapMapSetNFSA;

public final class FSAs
{
    private FSAs()
    {
    }

    public static <S> BasicFSABuilder<S> builder(int symbolNumberEstimate, S epsilonSymbol, int stateNumberEstimate)
    {
        return new BasicFSABuilder<>(symbolNumberEstimate, epsilonSymbol, stateNumberEstimate);
    }

    public static <S> BasicFSABuilder<S> builderBasedOn(FSA<S> fsa)
    {
        return fsa instanceof MapMapDFSA<?>
               ? new BasicFSABuilder<>((MapMapDFSA<S>) fsa)
               : new BasicFSABuilder<>((MapMapSetNFSA<S>) fsa);
    }

    public static <S> FSA<S> withEmptyLanguage(Alphabet<S> alphabet)
    {
        final State state = States.generate();
        final BasicFSABuilder<S> builder = builder(alphabet.size(), alphabet.getEpsilonSymbol(), 1);
        builder.addStartState(state);
        alphabet.getSet().forEach(symbol -> builder.addTransition(state, state, symbol));

        return builder.build();
    }

    public static <S> FSA<S> withSigmaStarLanguage(Alphabet<S> alphabet)
    {
        final State state = States.generate();
        final BasicFSABuilder<S> builder = builder(alphabet.size(), alphabet.getEpsilonSymbol(), 1);
        builder.addStartState(state).addAcceptState(state);
        alphabet.getSet().forEach(symbol -> builder.addTransition(state, state, symbol));

        return builder.build();
    }
}
