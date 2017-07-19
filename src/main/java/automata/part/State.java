package automata.part;

import org.eclipse.collections.api.list.ImmutableList;

public interface State
{
    boolean isDeterministic();

    boolean isNondeterministic();

    int getId();

    String getDisplayName();

    int getReferenceIndex();

    ImmutableList<? extends Transition<? extends Label>> getDTransitions();

    ImmutableList<? extends ImmutableList<? extends Transition<? extends Label>>> getNTransitions();
}
