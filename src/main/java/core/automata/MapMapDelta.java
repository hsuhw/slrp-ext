package core.automata;

import api.automata.Deterministic;
import api.automata.State;
import api.automata.Symbol;
import api.automata.TransitionFunction;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

public class MapMapDelta<S extends Symbol> implements Deterministic, TransitionFunction<S>
{
    private final ImmutableMap<State, ImmutableMap<S, State>> delta;
    private final ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> deltaInversed;

    private MapMapDelta(ImmutableMap<State, ImmutableMap<S, State>> definition,
                        ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> definitionInversed)
    {
        delta = definition;
        deltaInversed = definitionInversed;
    }

    private static <S extends Symbol> ImmutableMap<State, ImmutableMap<S, State>> immutableDefinition(
        MutableMap<State, MutableMap<S, State>> definition)
    {
        return definition.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.toImmutable());
        }).toImmutable();
    }

    private static <S extends Symbol> ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> immutableInversedDefinition(
        MutableMap<State, MutableMap<S, MutableSet<State>>> definition)
    {
        return definition.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.collect((sym, dests) -> {
                return Tuples.pair(sym, dests.toImmutable());
            }).toImmutable());
        }).toImmutable();
    }

    private static <S extends Symbol> MutableMap<State, MutableMap<S, MutableSet<State>>> computeInverse(
        MutableMap<State, MutableMap<S, State>> definition)
    {
        final MutableMap<State, MutableMap<S, MutableSet<State>>> inverse = UnifiedMap.newMap(definition.size());
        definition.forEach((dept, transes) -> {
            transes.forEach((sym, dest) -> {
                inverse.getIfAbsentPut(dest, UnifiedMap.newMap(definition.size()))
                       .getIfAbsentPut(sym, UnifiedSet.newSet(definition.size())) // upper bound
                       .add(dept);
            });
        });
        return inverse;
    }

    public MapMapDelta(MutableMap<State, MutableMap<S, State>> definition,
                       MutableMap<State, MutableMap<S, MutableSet<State>>> definitionInversed)
    {
        // TODO: decide whether to check the validity of `definitionInversed`
        this(immutableDefinition(definition), immutableInversedDefinition(definitionInversed));
    }

    public MapMapDelta(MutableMap<State, MutableMap<S, State>> definition)
    {
        this(definition, computeInverse(definition));
    }

    public MutableMap<State, MutableMap<S, State>> getMutableDefinition()
    {
        return delta.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.toMap());
        }).toMap();
    }

    public MutableMap<State, MutableMap<S, MutableSet<State>>> getMutableInversedDefinition()
    {
        return deltaInversed.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.collect((sym, dests) -> {
                return Tuples.pair(sym, dests.toSet());
            }).toMap());
        }).toMap();
    }

    @Override
    public int size()
    {
        return delta.collect(trans -> trans.count(Predicates.notNull())).injectInto(0, Integer::sum);
    }

    @Override
    public ImmutableSet<S> enabledSymbolsOn(State state)
    {
        return delta.get(state).keysView().toSet().toImmutable();
    }

    @Override
    public ImmutableSet<State> successorsOf(State state)
    {
        return delta.get(state).valuesView().toSet().toImmutable();
    }

    @Override
    public ImmutableSet<State> successorsOf(State state, S symbol)
    {
        return Sets.immutable.of(delta.get(state).get(symbol));
    }

    @Override
    public State successorOf(State state, S symbol)
    {
        return delta.get(state).get(symbol);
    }

    @Override
    public ImmutableSet<State> predecessorsOf(State state)
    {
        return deltaInversed.get(state).flatCollect(Functions.identity()).toSet().toImmutable();
    }

    @Override
    public ImmutableSet<State> predecessorsOf(State state, S symbol)
    {
        return deltaInversed.get(state).get(symbol);
    }

    @Override
    public String toString()
    {
        final String newline = System.getProperty("line.separator");
        final String indent = "  ";
        final StringBuilder layout = new StringBuilder();

        delta.forEachKeyValue((qi, stateTrans) -> {
            stateTrans.forEachKeyValue((symbol, qj) -> {
                layout.append(indent).append(qi);
                layout.append(" -> ").append(qj);
                layout.append(" [").append(symbol).append("];").append(newline);
            });
        });

        return layout.toString();
    }
}
