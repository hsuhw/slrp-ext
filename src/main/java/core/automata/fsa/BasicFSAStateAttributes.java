package core.automata.fsa;

import api.automata.State;
import api.automata.fsa.FSAManipulator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public class BasicFSAStateAttributes implements FSAManipulator.StateAttributes
{
    private final ImmutableList<State> definitionOfStates;
    private final ImmutableBooleanList startStateTable;
    private final ImmutableBooleanList acceptStateTable;

    public BasicFSAStateAttributes(ImmutableList<State> states, ImmutableBooleanList startStateTable,
                                   ImmutableBooleanList acceptStateTable)
    {
        definitionOfStates = states;
        this.startStateTable = startStateTable;
        this.acceptStateTable = acceptStateTable;
    }

    @Override
    public ImmutableList<State> getDefinitionOfStates()
    {
        return definitionOfStates;
    }

    @Override
    public ImmutableBooleanList getStartStateTable()
    {
        return startStateTable;
    }

    @Override
    public ImmutableBooleanList getAcceptStateTable()
    {
        return acceptStateTable;
    }
}
