package automata.part;

import org.eclipse.collections.api.list.ImmutableList;

import java.util.List;

public interface MutableState extends State
{
    void setDTransitions(ImmutableList<? extends Transition<? extends Label>> dTransitions);

    void setNTransitions(List<? extends ImmutableList<? extends Transition<? extends Label>>> nTransitions);
}
