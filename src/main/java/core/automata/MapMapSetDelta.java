package core.automata;

import api.automata.Nondeterministic;
import api.automata.State;
import api.automata.Symbol;
import api.automata.TransitionFunction;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

public class MapMapSetDelta<S extends Symbol> implements Nondeterministic, TransitionFunction<S>
{
    private final ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> delta;
    private final ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> deltaInversed;

    private MapMapSetDelta(ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> definition,
                           ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> definitionInversed)
    {
        delta = definition;
        deltaInversed = definitionInversed;
    }

    private static <S extends Symbol> ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> immutableDefinition(
        MutableMap<State, MutableMap<S, MutableSet<State>>> definition)
    {
        return definition.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.collect((sym, dests) -> {
                return Tuples.pair(sym, dests.toImmutable());
            }).toImmutable());
        }).toImmutable();
    }

    private static <S extends Symbol> MutableMap<State, MutableMap<S, MutableSet<State>>> computeInverse(
        MutableMap<State, MutableMap<S, MutableSet<State>>> definition)
    {
        final MutableMap<State, MutableMap<S, MutableSet<State>>> inverse = UnifiedMap.newMap(definition.size());
        definition.forEach((dept, transes) -> {
            transes.forEach((sym, dests) -> {
                dests.forEach(dest -> {
                    inverse.getIfAbsentPut(dest, UnifiedMap.newMap(definition.size()))
                           .getIfAbsentPut(sym, UnifiedSet.newSet(definition.size())) // upper bound
                           .add(dept);
                });
            });
        });
        return inverse;
    }

    public MapMapSetDelta(MutableMap<State, MutableMap<S, MutableSet<State>>> definition,
                          MutableMap<State, MutableMap<S, MutableSet<State>>> definitionInversed)
    {
        // TODO: decide whether to check the validity of `definitionInversed`
        this(immutableDefinition(definition), immutableDefinition(definitionInversed));
    }

    public MapMapSetDelta(MutableMap<State, MutableMap<S, MutableSet<State>>> definition)
    {
        this(definition, computeInverse(definition));
    }

    private static <S extends Symbol> MutableMap<State, MutableMap<S, MutableSet<State>>> mutableDefinition(
        ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> definition)
    {
        return definition.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.collect((sym, dests) -> {
                return Tuples.pair(sym, dests.toSet());
            }).toMap());
        }).toMap();
    }

    public MutableMap<State, MutableMap<S, MutableSet<State>>> getMutableDefinition()
    {
        return mutableDefinition(delta);
    }

    public MutableMap<State, MutableMap<S, MutableSet<State>>> getMutableInversedDefinition()
    {
        return mutableDefinition(deltaInversed);
    }

    @Override
    public int size()
    {
        return delta.flatCollect(trans -> trans.collect(ImmutableSet::size)).injectInto(0, Integer::sum);
    }

    @Override
    public ImmutableSet<S> enabledSymbolsOn(State state)
    {
        return delta.get(state).keysView().toSet().toImmutable();
    }

    private ImmutableSet<State> getSuccessorsFromGraph(ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> graph,
                                                       State state)
    {
        return graph.get(state).flatCollect(Functions.identity()).toSet().toImmutable();
    }

    private ImmutableSet<State> getSuccessorsFromGraph(ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> graph,
                                                       State state, S symbol)
    {
        return graph.get(state).get(symbol);
    }

    @Override
    public ImmutableSet<State> successorsOf(State state)
    {
        return getSuccessorsFromGraph(delta, state);
    }

    @Override
    public ImmutableSet<State> successorsOf(State state, S symbol)
    {
        return getSuccessorsFromGraph(delta, state, symbol);
    }

    @Override
    public ImmutableSet<State> predecessorsOf(State state)
    {
        return getSuccessorsFromGraph(deltaInversed, state);
    }

    @Override
    public ImmutableSet<State> predecessorsOf(State state, S symbol)
    {
        return getSuccessorsFromGraph(deltaInversed, state, symbol);
    }
}
