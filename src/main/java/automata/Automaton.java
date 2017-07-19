package automata;

import automata.part.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public interface Automaton
{
    boolean isDeterministic();

    boolean isNondeterministic();

    ImmutableList<State> getStates();

    ImmutableBooleanList getStartStateTable();

    boolean isStartState(int stateIndex);

    ImmutableBooleanList getAcceptStateTable();

    boolean isAcceptState(int stateIndex);
}
