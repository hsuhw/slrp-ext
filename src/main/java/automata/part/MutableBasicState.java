package automata.part;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.util.List;

public class MutableBasicState extends BasicState implements MutableState
{
    public MutableBasicState(int id, String displayName, int referenceIndex)
    {
        super(id, displayName, referenceIndex);
    }

    @Override
    public void setDTransitions(ImmutableList<? extends Transition<? extends Label>> dTransitions)
    {
        this.dTransitions = dTransitions;
        this.nTransitions = null;
        isDeterministic = true;
        hashCode = computeHashCode();
    }

    @Override
    public void setNTransitions(List<? extends ImmutableList<? extends Transition<? extends Label>>> nTransitions)
    {
        this.dTransitions = null;
        this.nTransitions = Lists.immutable.ofAll(nTransitions);
        isDeterministic = false;
        hashCode = computeHashCode();
    }
}
