package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

public interface MutableState<T> extends State<T>
{
    @Override
    RichIterable<? extends Pair<T, ? extends MutableState<T>>> transitions();

    @Override
    SetIterable<? extends MutableState<T>> successors();

    @Override
    SetIterable<? extends MutableState<T>> successors(T transLabel);

    @Override
    default MutableState<T> successor(T transLabel)
    {
        return successors().getOnly();
    }

    @Override
    default MutableState<T> toMutable()
    {
        return this;
    }

    MutableState<T> setName(String name);

    boolean addTransition(T transLabel, MutableState<T> to);

    MutableState<T> removeTransitionsTo(MutableState<T> state);

    @Override
    String toString();
}
