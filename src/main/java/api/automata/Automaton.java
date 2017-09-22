package api.automata;

import org.eclipse.collections.api.set.SetIterable;

public interface Automaton<S>
{
    SetIterable<State> getStates();

    default int getStateNumber()
    {
        return getStates().size();
    }

    SetIterable<State> getStartStates();

    default boolean isStartState(State state)
    {
        return getStartStates().contains(state);
    }

    SetIterable<State> getAcceptStates();

    default boolean isAcceptState(State state)
    {
        return getAcceptStates().contains(state);
    }

    DeltaFunction<S> getDeltaFunction();

    interface Builder<S>
    {
        void addState(State state);

        void addStartState(State state);

        void addAcceptState(State state);

        void addTransition(State dept, State dest, S symbol);

        Automaton<S> build();
    }
}
