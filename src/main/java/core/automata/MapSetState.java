package core.automata;

import api.automata.MutableState;
import api.automata.State;
import common.util.Assert;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.util.Constants.DISPLAY_DUMMY_STATE_NAME_PREFIX;
import static core.Parameters.NONDETERMINISTIC_TRANSITION_CAPACITY;

public class MapSetState<T> implements MutableState<T>
{
    private String name;
    private MutableMap<T, MutableSet<MutableState<T>>> transitions;

    public MapSetState(int transLabelCapacity)
    {
        transitions = UnifiedMap.newMap(transLabelCapacity);
    }

    private MutableSet<MutableState<T>> newDestinationSet()
    {
        return UnifiedSet.newSet(NONDETERMINISTIC_TRANSITION_CAPACITY);
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public RichIterable<? extends Pair<T, ? extends MutableState<T>>> transitions()
    {
        return transitions.keyValuesView().flatCollect(each -> {
            final T transLabel = each.getOne();
            return each.getTwo().collect(dest -> Tuples.pair(transLabel, dest));
        });
    }

    @Override
    public SetIterable<T> enabledSymbols()
    {
        return Sets.adapt(transitions.asUnmodifiable().keySet());
    }

    @Override
    public SetIterable<T> enabledSymbolsTo(State<T> state)
    {
        return Sets.adapt(transitions.select((__, dests) -> dests.contains(state)).keySet()); // one-off
    }

    @Override
    public boolean transitionExists(T transLabel)
    {
        return transitions.containsKey(transLabel);
    }

    @Override
    public boolean transitionExists(State<T> state)
    {
        return transitions.anySatisfyWith(SetIterable::contains, state);
    }

    @Override
    public SetIterable<? extends MutableState<T>> successors()
    {
        return transitions.flatCollect(x -> x).toSet(); // one-off
    }

    @Override
    public SetIterable<? extends MutableState<T>> successors(T transLabel)
    {
        return transitionExists(transLabel) ? transitions.get(transLabel).asUnmodifiable() : Sets.immutable.empty();
    }

    @Override
    public MutableState<T> setName(String value)
    {
        Assert.argumentNotNull(value);

        name = value;

        return this;
    }

    @Override
    public boolean addTransition(T transLabel, MutableState<T> to)
    {
        Assert.argumentNotNull(transLabel, to);

        return transitions.computeIfAbsent(transLabel, __ -> newDestinationSet()).add(to);
    }

    @Override
    public MutableState<T> removeTransitionsTo(MutableState<T> state)
    {
        transitions.forEach((transLabel, dests) -> {
            if (dests.remove(state) && dests.isEmpty()) {
                transitions.remove(transLabel);
            }
        });

        return this;
    }

    @Override
    public String toString()
    {
        final String nameTag = name != null ? name : DISPLAY_DUMMY_STATE_NAME_PREFIX + 0;

        return toString("", nameTag);
    }
}
