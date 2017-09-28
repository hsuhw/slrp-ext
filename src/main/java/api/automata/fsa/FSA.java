package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.DeltaFunction;
import api.automata.State;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

public interface FSA<S> extends Automaton<S>
{
    default SetIterable<State> getIncompleteStates()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final Alphabet<S> alphabet = getAlphabet();
        final DeltaFunction<S> delta = getDeltaFunction();
        final ImmutableSet<S> completeAlphabet = alphabet.getNoEpsilonSet();

        return getStates().select(state -> {
            return delta.enabledSymbolsOn(state).containsAllIterable(completeAlphabet);
        });
    }

    default boolean isComplete()
    {
        return getIncompleteStates().size() > 0;
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
        Builder<S> addStartStates(SetIterable<State> states);

        @Override
        Builder<S> resetStartStates();

        @Override
        Builder<S> addAcceptState(State state);

        @Override
        Builder<S> addAcceptStates(SetIterable<State> states);

        @Override
        Builder<S> resetAcceptStates();

        @Override
        Builder<S> addTransition(State dept, State dest, S symbol);

        @Override
        FSA<S> build();

        FSA<S> build(Alphabet<S> alphabet);
    }
}
