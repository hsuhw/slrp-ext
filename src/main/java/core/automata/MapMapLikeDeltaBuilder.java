package core.automata;

import api.automata.DeltaFunction;
import api.automata.State;
import core.util.Assertions;
import core.util.Parameters;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.DeltaFunction.Builder;

public final class MapMapLikeDeltaBuilder<S> implements Builder<S>
{
    private final S epsilonSymbol;
    private final MutableMap<State, MutableMap<S, MutableSet<State>>> forwardDelta;
    private final MutableMap<State, MutableMap<S, MutableSet<State>>> backwardDelta;

    public MapMapLikeDeltaBuilder(int stateNumberEstimate, S epsilonSymbol)
    {
        Assertions.argumentNotNull(epsilonSymbol);

        forwardDelta = UnifiedMap.newMap(stateNumberEstimate);
        backwardDelta = UnifiedMap.newMap(stateNumberEstimate);
        this.epsilonSymbol = epsilonSymbol;
    }

    private static int estimateStateNumber(int originalSize)
    {
        return (int) Math.round(originalSize * Parameters.ADDITIONAL_STATE_CAPACITY_MULTIPLIER);
    }

    private static <S> MutableMap<S, MutableSet<State>> newStateTrans()
    {
        return UnifiedMap.newMap(Parameters.CURRENT_BIGGEST_ALPHABET_SIZE);
    }

    private static MutableSet<State> newStateSet()
    {
        return UnifiedSet.newSet(Parameters.NONDETERMINISTIC_TRANSITION_CAPACITY);
    }

    private static <S> MutableMap<State, MutableMap<S, MutableSet<State>>> mutableDelta(
        ImmutableMap<State, ImmutableMap<S, ImmutableSet<State>>> definition, int stateNumberEstimate)
    {
        final MutableMap<State, MutableMap<S, MutableSet<State>>> result = UnifiedMap.newMap(stateNumberEstimate);
        definition.forEachKeyValue((dept, transes) -> {
            final MutableMap<S, MutableSet<State>> stateTrans = newStateTrans();
            transes.forEachKeyValue((sym, dests) -> {
                final MutableSet<State> destSet = newStateSet();
                destSet.addAll(dests.castToSet());
                stateTrans.put(sym, destSet);
            });
            result.put(dept, stateTrans);
        });

        return result;
    }

    public MapMapLikeDeltaBuilder(MapMapSetDelta<S> delta)
    {
        final int estimate = estimateStateNumber(delta.getAllReferredStates().size());
        forwardDelta = mutableDelta(delta.getForwardDelta(), estimate);
        backwardDelta = mutableDelta(delta.getBackwardDelta(), estimate);
        epsilonSymbol = delta.getEpsilonSymbol();
    }

    private static <S> MutableMap<State, MutableMap<S, MutableSet<State>>> mutableHoistedDelta(
        ImmutableMap<State, ImmutableMap<S, State>> definition, int stateNumberEstimate)
    {
        final MutableMap<State, MutableMap<S, MutableSet<State>>> result = UnifiedMap.newMap(stateNumberEstimate);
        definition.forEachKeyValue((dept, transes) -> {
            final MutableMap<S, MutableSet<State>> stateTrans = newStateTrans();
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
        final int estimate = estimateStateNumber(delta.getAllReferredStates().size());
        forwardDelta = mutableHoistedDelta(delta.getForwardDelta(), estimate);
        backwardDelta = mutableDelta(delta.getBackwardDelta(), estimate);
        epsilonSymbol = delta.getEpsilonSymbol();
    }

    @Override
    public Builder<S> addState(State state)
    {
        Assertions.argumentNotNull(state);

        forwardDelta.computeIfAbsent(state, __ -> newStateTrans());
        backwardDelta.computeIfAbsent(state, __ -> newStateTrans());

        return this;
    }

    private void removeStateAndEmptyMap(MutableMap<S, MutableSet<State>> fromState, S symbol, State target)
    {
        fromState.get(symbol).remove(target);
        if (fromState.get(symbol).isEmpty()) {
            fromState.remove(symbol);
        }
    }

    @Override
    public Builder<S> removeState(State state)
    {
        if (!forwardDelta.containsKey(state)) {
            return this;
        }

        forwardDelta.get(state).forEach((symbol, dests) -> {
            dests.forEach(dest -> removeStateAndEmptyMap(backwardDelta.get(dest), symbol, state));
        });
        forwardDelta.remove(state);
        backwardDelta.get(state).forEach((symbol, depts) -> {
            depts.forEach(dept -> removeStateAndEmptyMap(forwardDelta.get(dept), symbol, state));
        });
        backwardDelta.remove(state);

        return this;
    }

    @Override
    public Builder<S> addTransition(State dept, State dest, S symbol)
    {
        Assertions.argumentNotNull(dept, dest, symbol);

        forwardDelta.get(dept).computeIfAbsent(symbol, __ -> newStateSet()).add(dest);

        return this;
    }

    private boolean isNondeterministic()
    {
        return forwardDelta.anySatisfy(stateTrans -> {
            return stateTrans.containsKey(epsilonSymbol) || stateTrans.anySatisfy(that -> that.size() > 1);
        });
    }

    @Override
    public DeltaFunction<S> build()
    {
        return isNondeterministic() ? new MapMapSetDelta<>(this) : new MapMapDelta<>(this);
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
