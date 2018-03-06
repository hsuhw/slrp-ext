package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

public interface MutableState<S> extends State<S>
{
    @Override
    RichIterable<Pair<S, State<S>>> transitions();

    @Override
    SetIterable<State<S>> successors();

    @Override
    SetIterable<State<S>> successors(S transLabel);

    @Override
    default State<S> successor(S transLabel)
    {
        return successors().getOnly();
    }

    @Override
    default MutableState<S> toMutable()
    {
        return this;
    }

    MutableState<S> setName(String name);

    boolean addTransition(S transLabel, MutableState<S> to);

    MutableState<S> removeTransitionsTo(MutableState<S> state);

    @Override
    String toString();
}
