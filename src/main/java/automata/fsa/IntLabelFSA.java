package automata.fsa;

import automata.AutomatonAbst;
import automata.part.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public class IntLabelFSA extends AutomatonAbst implements FSA
{
    public IntLabelFSA(ImmutableList<State> states, ImmutableBooleanList startStateTable,
                       ImmutableBooleanList acceptStateTable)
    {
        super(states, startStateTable, acceptStateTable);
    }
}
