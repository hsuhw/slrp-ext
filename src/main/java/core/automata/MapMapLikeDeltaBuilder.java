package core.automata;

import api.automata.DeltaFunction;
import api.automata.State;
import core.util.Assertions;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.DeltaFunction.Builder;
import static core.util.Parameters.NONDETERMINISTIC_TRANSITION_CAPACITY;
import static core.util.Parameters.estimateExtendedSize;

public final class MapMapLikeDeltaBuilder<S> implements DeltaFunction.Builder<S>
{
    private final int symbolNumberEstimate;
    private final MutableMap<State, MutableMap<S, MutableSet<State>>> forwardDelta;
    private final S epsilonSymbol;
    private final MutableMap<State, MutableMap<S, MutableSet<State>>> backwardDelta;

    public MapMapLikeDeltaBuilder(int stateNumberEstimate, int symbolNumberEstimate, S epsilonSymbol)
    {
        Assertions.argumentNotNull(epsilonSymbol);

        forwardDelta = UnifiedMap.newMap(stateNumberEstimate);
        backwardDelta = UnifiedMap.newMap(stateNumberEstimate);
        this.symbolNumberEstimate = symbolNumberEstimate;
        this.epsilonSymbol = epsilonSymbol;
    }

    private static MutableSet<State> newStateSet()
    {
        return UnifiedSet.newSet(NONDETERMINISTIC_TRANSITION_CAPACITY);
    }

    private static <S> MutableMap<State, MutableMap<S, MutableSet<State>>> mutableDelta(
        ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> definition, int stateNumberEstimate,
        int symbolNumberEstimate)
    {
        final MutableMap<State, MutableMap<S, MutableSet<State>>> result = UnifiedMap.newMap(stateNumberEstimate);
        definition.forEachKeyValue((dept, transes) -> {
            final MutableMap<S, MutableSet<State>> stateTrans = UnifiedMap.newMap(symbolNumberEstimate);
            transes.forEachKeyValue((sym, dests) -> {
                final MutableSet<State> destSet = newStateSet();
                destSet.addAllIterable(dests);
                stateTrans.put(sym, destSet);
            });
            result.put(dept, stateTrans);
        });

        return result;
    }

    public MapMapLikeDeltaBuilder(MapMapSetDelta<S> delta)
    {
        final int estimateStateNumber = estimateExtendedSize(delta.getAllReferredStates().size());
        symbolNumberEstimate = estimateExtendedSize(delta.getAllReferredSymbols().size());
        forwardDelta = mutableDelta(delta.getForwardDelta(), estimateStateNumber, symbolNumberEstimate);
        backwardDelta = mutableDelta(delta.getBackwardDelta(), estimateStateNumber, symbolNumberEstimate);
        epsilonSymbol = delta.getEpsilonSymbol();
    }

    private static <S> MutableMap<State, MutableMap<S, MutableSet<State>>> mutableHoistedDelta(
        ImmutableMap<State, ImmutableMap<S, State>> definition, int stateNumberEstimate, int symbolNumberEstimate)
    {
        final MutableMap<State, MutableMap<S, MutableSet<State>>> result = UnifiedMap.newMap(stateNumberEstimate);
        definition.forEachKeyValue((dept, transes) -> {
            final MutableMap<S, MutableSet<State>> stateTrans = UnifiedMap.newMap(symbolNumberEstimate);
            transes.forEachKeyValue((sym, dest) -> {
                final MutableSet<State> dests = newStateSet();
                dests.add(dest);
                stateTrans.put(sym, dests);
            });
            result.put(dept, stateTrans);
        });

        return result;
    }

    public MapMapLikeDeltaBuilder(MapMapDelta<S> delta)
    {
        final int estimateStateNumber = estimateExtendedSize(delta.getAllReferredStates().size());
        symbolNumberEstimate = estimateExtendedSize(delta.getAllReferredSymbols().size());
        forwardDelta = mutableHoistedDelta(delta.getForwardDelta(), estimateStateNumber, symbolNumberEstimate);
        backwardDelta = mutableDelta(delta.getBackwardDelta(), estimateStateNumber, symbolNumberEstimate);
        epsilonSymbol = delta.getEpsilonSymbol();
    }

    private void removeEdgeFromGraph(MutableMap<State, MutableMap<S, MutableSet<State>>> graph, State from, S symbol,
                                     State to)
    {
        final MutableMap<S, MutableSet<State>> stateTrans = graph.get(from);
        final MutableSet<State> stateSet = stateTrans.get(symbol);
        stateSet.remove(to);
        if (stateSet.isEmpty()) {
            stateTrans.remove(symbol);
        }
        if (stateTrans.isEmpty()) {
            graph.remove(from);
        }
    }

    @Override
    public Builder<S> removeState(State state)
    {
        if (!forwardDelta.containsKey(state)) {
            return this;
        }

        forwardDelta.get(state).forEach((symbol, dests) -> {
            dests.forEach(dest -> removeEdgeFromGraph(backwardDelta, dest, symbol, state));
        });
        forwardDelta.remove(state);
        backwardDelta.get(state).forEach((symbol, depts) -> {
            depts.forEach(dept -> removeEdgeFromGraph(forwardDelta, dept, symbol, state));
        });
        backwardDelta.remove(state);

        return this;
    }

    private <S> MutableMap<S, MutableSet<State>> newStateTrans()
    {
        return UnifiedMap.newMap(symbolNumberEstimate);
    }

    @Override
    public Builder<S> addTransition(State dept, State dest, S symbol)
    {
        Assertions.argumentNotNull(dept, dest, symbol);

        forwardDelta.computeIfAbsent(dept, __ -> newStateTrans()) // heuristic
                    .computeIfAbsent(symbol, __ -> newStateSet()) // heuristic
                    .add(dest);
        backwardDelta.computeIfAbsent(dest, __ -> newStateTrans()) // heuristic
                     .computeIfAbsent(symbol, __ -> newStateSet()) // heuristic
                     .add(dept);

        return this;
    }

    @Override
    public Builder<S> removeTransition(State dept, State dest, S symbol)
    {
        Assertions.argumentNotNull(dept, dest, symbol);

        removeEdgeFromGraph(forwardDelta, dept, symbol, dest);
        removeEdgeFromGraph(backwardDelta, dest, symbol, dept);

        return this;
    }

    private boolean isNondeterministic()
    {
        return forwardDelta.detect((state, stateTrans) -> {
            final MutableSet<State> epsilonDests = stateTrans.get(epsilonSymbol);
            final boolean nonLoopEpsilonTrans = epsilonDests != null //
                && (epsilonDests.size() > 1 || epsilonDests.getOnly() != state);
            return nonLoopEpsilonTrans || stateTrans.anySatisfy(that -> that.size() > 1);
        }) != null;
    }

    @Override
    public DeltaFunction<S> build()
    {
        return build(false);
    }

    @Override
    public DeltaFunction<S> build(boolean generalized)
    {
        if (forwardDelta.isEmpty()) {
            throw new IllegalStateException("nothing has been specified");
        }

        return generalized || isNondeterministic() ? new MapMapSetDelta<>(this) : new MapMapDelta<>(this);
    }

    private ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> immutableDelta(
        MutableMap<State, MutableMap<S, MutableSet<State>>> definition)
    {
        return definition.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.collect((sym, dests) -> {
                return Tuples.pair(sym, dests.toImmutable());
            }).toImmutable());
        }).toImmutable();
    }

    private ImmutableMap<State, ImmutableMap<S, State>> immutableDemotedDelta(
        MutableMap<State, MutableMap<S, MutableSet<State>>> definition)
    {
        return definition.collect((dept, transes) -> {
            return Tuples.pair(dept, transes.collect((sym, dests) -> {
                return Tuples.pair(sym, dests.getOnly());
            }).toImmutable());
        }).toImmutable();
    }

    ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> getForwardDelta()
    {
        return immutableDelta(forwardDelta);
    }

    ImmutableMap<State, ImmutableMap<S, State>> getDemotedForwardDelta()
    {
        return immutableDemotedDelta(forwardDelta);
    }

    ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> getBackwardDelta()
    {
        return immutableDelta(backwardDelta);
    }

    S getEpsilonSymbol()
    {
        return epsilonSymbol;
    }
}
