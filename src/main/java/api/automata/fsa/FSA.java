package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;

public interface FSA<S> extends Automaton<S>
{
    default boolean isComplete()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("N/A on nondeterministic FSA");
        }

        final int completeSize = getAlphabet().size() - 1; // without epsilon
        for (State s : getStates()) {
            if (getDeltaFunction().enabledSymbolsOn(s).size() != completeSize) {
                return false;
            }
        }

        return true;
    }

    interface Builder<S> extends Automaton.Builder<S>
    {
        Alphabet<S> getCurrentAlphabet();

        @Override
        Builder<S> addSymbol(S symbol);

        @Override
        Builder<S> addState(State state);

        @Override
        Builder<S> removeState(State state);

        @Override
        Builder<S> addStartState(State state);

        @Override
        Builder<S> addAcceptState(State state);

        @Override
        Builder<S> addTransition(State dept, State dest, S symbol);

        @Override
        FSA<S> build();

        FSA<S> build(Alphabet<S> alphabet);
    }
}
