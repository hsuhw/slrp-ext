package core.automata;

import api.automata.DeltaFunction;
import api.automata.Nondeterministic;
import api.automata.State;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;
import static core.util.Parameters.estimateExtendedSize;

public final class MapMapSetDelta<S> implements Nondeterministic, DeltaFunction<S>
{
    private final S epsilonSymbol;
    private final ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> forwardDelta;
    private final ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> backwardDelta;

    private MapMapSetDelta(ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> forwardDefinition,
                           ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> backwardDefinition,
                           S epsilonSymbol)
    {
        forwardDelta = forwardDefinition;
        backwardDelta = backwardDefinition;
        this.epsilonSymbol = epsilonSymbol;
    }

    public MapMapSetDelta(MapMapLikeDeltaBuilder<S> record)
    {
        this(record.getForwardDelta(), record.getBackwardDelta(), record.getEpsilonSymbol());
    }

    ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> getForwardDelta()
    {
        return forwardDelta;
    }

    ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> getBackwardDelta()
    {
        return backwardDelta;
    }

    @Override
    public int size()
    {
        return forwardDelta.flatCollect(trans -> trans.collect(RichIterable::size)).injectInto(0, Integer::sum);
    }

    @Override
    public SetIterable<State> getAllReferredStates()
    {
        return Sets.union(forwardDelta.keysView().toSet(), backwardDelta.keysView().toSet());
    }

    @Override
    public SetIterable<S> getAllReferredSymbols()
    {
        return forwardDelta.flatCollect(MapIterable::keysView).toSet();
    }

    @Override
    public S getEpsilonSymbol()
    {
        return epsilonSymbol;
    }

    @Override
    public SetIterable<S> enabledSymbolsOn(State state)
    {
        if (!forwardDelta.containsKey(state)) {
            return Sets.immutable.empty();
        }

        return forwardDelta.get(state).keysView().toSet();
    }

    @Override
    public boolean available(State state, S symbol)
    {
        return forwardDelta.containsKey(state) && forwardDelta.get(state).get(symbol) != null;
    }

    private SetIterable<State> getSuccessorsFromGraph(
        MapIterable<State, ? extends MapIterable<S, ? extends SetIterable<State>>> graph, State state)
    {
        if (!graph.containsKey(state)) {
            return Sets.immutable.empty();
        }

        return graph.get(state).flatCollect(Functions.identity()).toSet();
    }

    @Override
    public SetIterable<State> successorsOf(State state)
    {
        return getSuccessorsFromGraph(forwardDelta, state);
    }

    private SetIterable<State> getSuccessorsFromGraph(
        MapIterable<State, ? extends MapIterable<S, ? extends SetIterable<State>>> graph, State state, S symbol)
    {
        if (graph.containsKey(state) && graph.get(state).containsKey(symbol)) {
            return graph.get(state).get(symbol).toImmutable(); // defense required
        }
        return Sets.immutable.empty();
    }

    @Override
    public SetIterable<State> successorsOf(State state, S symbol)
    {
        return getSuccessorsFromGraph(forwardDelta, state, symbol);
    }

    private MutableSet<State> successorsOf(SetIterable<State> states, S symbol)
    {
        return states.flatCollect(state -> successorsOf(state, symbol)).toSet();
    }

    @Override
    public SetIterable<State> predecessorsOf(State state)
    {
        return getSuccessorsFromGraph(backwardDelta, state);
    }

    @Override
    public SetIterable<State> predecessorsOf(State state, S symbol)
    {
        return getSuccessorsFromGraph(backwardDelta, state, symbol);
    }

    @Override
    public SetIterable<State> epsilonClosureOf(SetIterable<State> states)
    {
        return epsilonClosureOf(states, epsilonSymbol);
    }

    @Override
    public SetIterable<State> epsilonClosureOf(SetIterable<State> states, S symbol)
    {
        MutableSet<State> baseSet = UnifiedSet.newSet(estimateExtendedSize(states.size())); // heuristic
        baseSet.addAllIterable(states);
        MutableSet<State> currSet;
        while (!baseSet.containsAll(currSet = successorsOf(baseSet, epsilonSymbol))) {
            baseSet.addAllIterable(currSet);
        }

        return symbol.equals(epsilonSymbol) ? baseSet : epsilonClosureOf(successorsOf(baseSet, symbol));
    }

    @Override
    public String toString()
    {
        final StringBuilder layout = new StringBuilder();
        forwardDelta.forEachKeyValue((dept, stateTrans) -> {
            stateTrans.forEachKeyValue((symbol, dests) -> {
                dests.forEach(dest -> {
                    layout.append(DISPLAY_INDENT);
                    layout.append(dept).append(" -> ").append(dest).append(" [").append(symbol).append("];");
                    layout.append(DISPLAY_NEWLINE);
                });
            });
        });

        return layout.toString();
    }
}
