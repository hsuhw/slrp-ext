package automata.part;

import org.eclipse.collections.api.list.ImmutableList;

import java.util.List;

public interface MutableState extends State
{
    @Override
    boolean isDeterministic();

    @Override
    boolean isNondeterministic();

    @Override
    int getId();

    @Override
    String getDisplayName();

    @Override
    int getReferenceIndex();

    @Override
    ImmutableList<? extends Transition<? extends Label>> getDTransitions();

    void setDTransitions(ImmutableList<? extends Transition<? extends Label>> dTransitions);

    @Override
    ImmutableList<? extends ImmutableList<? extends Transition<? extends Label>>> getNTransitions();

    void setNTransitions(List<? extends ImmutableList<? extends Transition<? extends Label>>> nTransitions);
}
