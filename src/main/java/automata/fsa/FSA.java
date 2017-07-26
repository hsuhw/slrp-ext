package automata.fsa;

import automata.Automaton;
import automata.part.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public interface FSA extends Automaton
{
    @Override
    boolean isDeterministic();

    @Override
    boolean isNondeterministic();

    @Override
    ImmutableList<State> getStates();

    @Override
    ImmutableBooleanList getStartStateTable();

    @Override
    boolean isStartState(int stateIndex);

    @Override
    ImmutableBooleanList getAcceptStateTable();

    @Override
    boolean isAcceptState(int stateIndex);
}
