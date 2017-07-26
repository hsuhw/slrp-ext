package automata.fsa;

import automata.AbstractAutomaton;
import automata.part.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public class IntLabelRelationDefiner extends AbstractAutomaton implements FSA
{
    public IntLabelRelationDefiner(ImmutableList<State> states, ImmutableBooleanList startStateTable,
                                   ImmutableBooleanList acceptStateTable)
    {
        super(states, startStateTable, acceptStateTable);
    }
}
