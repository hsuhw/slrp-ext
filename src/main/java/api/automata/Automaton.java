package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;

public interface Automaton<S>
{
    Alphabet<S> alphabet();

    ImmutableSet<State> states();

    ImmutableSet<State> startStates();

    default State startState()
    {
        if (startStates().size() != 1) {
            throw new UnsupportedOperationException("more than one start states");
        }

        return startStates().getOnly();
    }

    default boolean isStartState(State state)
    {
        return startStates().contains(state);
    }

    ImmutableSet<State> acceptStates();

    default boolean isAcceptState(State state)
    {
        return acceptStates().contains(state);
    }

    default ImmutableSet<State> nonAcceptStates()
    {
        return states().newWithoutAll(acceptStates());
    }

    TransitionGraph<State, S> transitionGraph();

    @Override
    String toString();

    interface Builder<S>
    {
        Builder<S> addSymbol(S symbol);

        Builder<S> addState(State state);

        Builder<S> removeState(State state);

        Builder<S> addStartState(State state);

        Builder<S> addStartStates(ImmutableSet<State> states);

        Builder<S> resetStartStates();

        Builder<S> addAcceptState(State state);

        Builder<S> addAcceptStates(ImmutableSet<State> states);

        Builder<S> resetAcceptStates();

        Builder<S> addTransition(State dept, State dest, S symbol);

        Automaton<S> build();
    }
}
