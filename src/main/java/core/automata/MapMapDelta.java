package core.automata;

import api.automata.DeltaFunction;
import api.automata.Deterministic;
import api.automata.State;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Sets;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public final class MapMapDelta<S> implements Deterministic, DeltaFunction<S>
{
    private final S epsilonSymbol;
    private final ImmutableMap<State, ImmutableMap<S, State>> forwardDelta;
    private final ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> backwardDelta;

    private MapMapDelta(ImmutableMap<State, ImmutableMap<S, State>> forwardDefinition,
                        ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> backwardDefinition, S epsilonSymbol)
    {
        forwardDelta = forwardDefinition;
        backwardDelta = backwardDefinition;
        this.epsilonSymbol = epsilonSymbol;
    }

    public MapMapDelta(MapMapLikeDeltaBuilder<S> record)
    {
        this(record.getDemotedForwardDelta(), record.getBackwardDelta(), record.getEpsilonSymbol());
    }

    ImmutableMap<State, ImmutableMap<S, State>> getForwardDelta()
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
        return forwardDelta.collect(RichIterable::size).injectInto(0, Integer::sum);
    }

    @Override
    public SetIterable<State> getAllReferredStates()
    {
        return Sets.union(forwardDelta.keysView().toSet(), getBackwardDelta().keysView().toSet());
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

    @Override
    public SetIterable<State> successorsOf(State state)
    {
        if (!forwardDelta.containsKey(state)) {
            return Sets.immutable.empty();
        }

        return forwardDelta.get(state).valuesView().toSet();
    }

    @Override
    public SetIterable<State> successorsOf(State state, S symbol)
    {
        if (!forwardDelta.containsKey(state)) {
            return Sets.immutable.empty();
        }

        return Sets.immutable.of(forwardDelta.get(state).get(symbol));
    }

    @Override
    public State successorOf(State state, S symbol)
    {
        return forwardDelta.get(state).get(symbol);
    }

    @Override
    public SetIterable<State> predecessorsOf(State state)
    {
        if (!backwardDelta.containsKey(state)) {
            return Sets.immutable.empty();
        }

        return backwardDelta.get(state).flatCollect(Functions.identity()).toSet();
    }

    @Override
    public SetIterable<State> predecessorsOf(State state, S symbol)
    {
        if (backwardDelta.containsKey(state) && backwardDelta.get(state).containsKey(symbol)) {
            return backwardDelta.get(state).get(symbol).toImmutable(); // defense required
        }
        return Sets.immutable.empty();
    }

    @Override
    public String toString()
    {
        final StringBuilder layout = new StringBuilder();
        forwardDelta.forEachKeyValue((dept, stateTrans) -> {
            stateTrans.forEachKeyValue((symbol, dest) -> {
                layout.append(DISPLAY_INDENT);
                layout.append(dept).append(" -> ").append(dest).append(" [").append(symbol).append("];");
                layout.append(DISPLAY_NEWLINE);
            });
        });

        return layout.toString();
    }
}
