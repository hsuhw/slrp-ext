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

import static core.Parameters.NONDETERMINISTIC_TRANSITION_CAPACITY;

public class MapSetState<S> implements MutableState<S>
{
    private String name;
    private MutableMap<S, MutableSet<MutableState<S>>> transitions;

    public MapSetState(int transLabelCapacity)
    {
        transitions = UnifiedMap.newMap(transLabelCapacity);
    }

    private MutableSet<MutableState<S>> newDestinationSet()
    {
        return UnifiedSet.newSet(NONDETERMINISTIC_TRANSITION_CAPACITY);
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public RichIterable<Pair<S, State<S>>> transitions()
    {
        return transitions.keyValuesView().flatCollect(each -> {
            final S transLabel = each.getOne();
            return each.getTwo().collect(dest -> Tuples.pair(transLabel, dest));
        });
    }

    @Override
    public SetIterable<S> enabledSymbols()
    {
        return Sets.adapt(transitions.asUnmodifiable().keySet());
    }

    @Override
    public SetIterable<S> enabledSymbolsTo(State<S> state)
    {
        return Sets.adapt(transitions.select((__, dests) -> dests.contains(state)).keySet()); // one-off
    }

    @Override
    public boolean transitionExists(S transLabel)
    {
        return transitions.containsKey(transLabel);
    }

    @Override
    public boolean transitionExists(State<S> state)
    {
        return transitions.anySatisfyWith(SetIterable::contains, state);
    }

    @Override
    public SetIterable<State<S>> successors()
    {
        @SuppressWarnings("unchecked")
        final MutableSet<State<S>> result = (MutableSet) transitions.flatCollect(x -> x).toSet(); // one-off
        return result;
    }

    @Override
    public SetIterable<State<S>> successors(S transLabel)
    {
        if (transitionExists(transLabel)) {
            @SuppressWarnings("unchecked")
            final MutableSet<State<S>> result = (MutableSet) transitions.get(transLabel);
            return result.asUnmodifiable();
        }

        return Sets.immutable.empty();
    }

    @Override
    public MutableState<S> setName(String value)
    {
        Assert.argumentNotNull(value);

        name = value;

        return this;
    }

    @Override
    public boolean addTransition(S transLabel, MutableState<S> to)
    {
        Assert.argumentNotNull(transLabel, to);

        return transitions.computeIfAbsent(transLabel, __ -> newDestinationSet()).add(to);
    }

    @Override
    public MutableState<S> removeTransitionsTo(MutableState<S> state)
    {
        Assert.argumentNotNull(state);

        transitions.forEach((transLabel, dests) -> {
            if (dests.remove(state) && dests.isEmpty()) {
                transitions.remove(transLabel);
            }
        });

        return this;
    }
}
