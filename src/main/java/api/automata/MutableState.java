package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;

public interface MutableState<S> extends State<S>
{
    @Override
    default MutableState<S> toMutable()
    {
        return this;
    }

    MutableState<S> setName(String name);

    MutableState<S> addTransition(S transLabel, MutableState<S> to);

    default MutableState<S> addTransitions(RichIterable<Pair<S, State<S>>> transitions)
    {
        transitions.forEach(trans -> addTransition(trans.getOne(), (MutableState<S>) trans.getTwo()));

        return this;
    }

    MutableState<S> removeTransition(S transLabel, MutableState<S> to);

    MutableState<S> removeTransitionsTo(MutableState<S> state);

    @Override
    String toString();
}
