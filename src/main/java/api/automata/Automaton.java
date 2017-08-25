package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;

public interface Automaton<S extends Symbol>
{
    ImmutableList<State> getStates();

    ImmutableObjectIntMap<State> getStateIndexTable();

    default int getStateNumber()
    {
        return getStates().size();
    }

    default State getState(int stateIndex)
    {
        return getStates().get(stateIndex);
    }

    default int getStateIndex(State state)
    {
        return getStateIndexTable().get(state);
    }

    ImmutableBooleanList getStartStateTable();

    default boolean isStartState(State state)
    {
        return getStartStateTable().get(getStateIndex(state));
    }

    default boolean isStartState(int stateIndex)
    {
        return getStartStateTable().get(stateIndex);
    }

    ImmutableBooleanList getAcceptStateTable();

    default boolean isAcceptState(State state)
    {
        return getAcceptStateTable().get(getStateIndex(state));
    }

    default boolean isAcceptState(int stateIndex)
    {
        return getAcceptStateTable().get(stateIndex);
    }

    TransitionFunction<S> getTransitionFunction();
}
