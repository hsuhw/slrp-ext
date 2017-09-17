package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import core.automata.States;

public final class FSAs
{
    private FSAs()
    {
    }

    public static <S extends Symbol> FSA<S> withEmptyLanguage(Alphabet<S> alphabet)
    {
        BasicFSABuilder<S> builder = new BasicFSABuilder<>(alphabet.size(), null, 1);
        State state = States.generateOne();
        builder.addStartState(state);
        alphabet.toSet().forEach(symbol -> builder.addTransition(state, state, symbol));
        return builder.build(alphabet);
    }

    public static <S extends Symbol> FSA<S> withSigmaStarLanguage(Alphabet<S> alphabet)
    {
        BasicFSABuilder<S> builder = new BasicFSABuilder<>(alphabet.size(), null, 1);
        State state = States.generateOne();
        builder.addStartState(state);
        builder.addAcceptState(state);
        alphabet.toSet().forEach(symbol -> builder.addTransition(state, state, symbol));
        return builder.build(alphabet);
    }
}
