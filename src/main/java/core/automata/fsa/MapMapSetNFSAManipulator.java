package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAManipulatorDecorator;
import core.automata.MapMapSetDelta;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import util.Misc;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class MapMapSetNFSAManipulator extends AbstractBasicFSAManipulator implements FSAManipulatorDecorator
{
    public MapMapSetNFSAManipulator(FSAManipulator decoratee)
    {
        super(decoratee);
    }

    private <S extends Symbol> boolean isImplementationTarget(Automaton<S> target)
    {
        return target instanceof MapMapSetNFSA<?>;
    }

    private <S extends Symbol> FSA<S> remakeFSAWithSubsetDelta(MapMapSetNFSA<S> target,
                                                               MutableMap<State, MutableMap<S, MutableSet<State>>> delta,
                                                               MutableMap<State, MutableMap<S, MutableSet<State>>> deltaInversed)
    {
        final MapMapSetDelta<S> newDelta = new MapMapSetDelta<>(delta, deltaInversed);
        final StateAttributes attributes = decideStateAttributes(target, delta);
        return new MapMapSetNFSA<>(target.getAlphabet(), attributes.getDefinitionOfStates(),
                                   attributes.getStartStateTable(), attributes.getAcceptStateTable(), newDelta);
    }

    private enum TrimmingTarget
    {
        UNREACHABLE, DEAD_END
    }

    private <S extends Symbol> FSA<S> trimStates(MapMapSetNFSA<S> target, TrimmingTarget trimmingTarget)
    {
        final MapMapSetDelta<S> transes = target.getTransitionFunction();

        // mark all the reachable states
        final Predicate<State> initialCondition;
        final Function<State, ImmutableSet<State>> stepFunction;
        if (trimmingTarget == TrimmingTarget.UNREACHABLE) {
            initialCondition = target::isStartState;
            stepFunction = transes::successorsOf;
        } else {
            initialCondition = target::isAcceptState;
            stepFunction = transes::predecessorsOf;
        }
        final int stateNumber = target.getStateNumber();
        final MutableSet<State> reachable = UnifiedSet.newSet(stateNumber); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();
        prepareStateReachabilitySearch(target.getStates(), initialCondition, reachable, pendingChecks);
        computeStateReachability(stepFunction, reachable, pendingChecks);

        // exclude the unreachable from the delta definition
        if (reachable.size() == stateNumber) {
            return target;
        }
        final ImmutableList<State> unreachable = target.getStates().newWithoutAll(reachable);
        final MutableMap<State, MutableMap<S, MutableSet<State>>> forwardDelta;
        final MutableMap<State, MutableMap<S, MutableSet<State>>> backwardDelta;
        if (trimmingTarget == TrimmingTarget.UNREACHABLE) {
            forwardDelta = transes.getMutableDefinition();
            backwardDelta = transes.getMutableInversedDefinition();
        } else {
            forwardDelta = transes.getMutableInversedDefinition();
            backwardDelta = transes.getMutableDefinition();
        }
        unreachable.forEach(state -> {
            forwardDelta.get(state).forEach((symbol, dests) -> {
                dests.forEach(dest -> backwardDelta.get(dest).get(symbol).remove(state));
            });
            forwardDelta.remove(state);
            backwardDelta.remove(state);
        });

        return remakeFSAWithSubsetDelta(target, forwardDelta, backwardDelta);
    }

    @Override
    public <S extends Symbol> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return trimStates((MapMapSetNFSA<S>) target, TrimmingTarget.UNREACHABLE);
    }

    @Override
    public <S extends Symbol> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return trimStates((MapMapSetNFSA<S>) target, TrimmingTarget.DEAD_END);
    }

    @Override
    public <S extends Symbol, T extends Symbol, R extends Symbol> FSA<R> makeProductDelegated(Automaton<S> one,
                                                                                              Automaton<T> two,
                                                                                              Alphabet<R> targetAlphabet,
                                                                                              BiFunction<S, T, R> transitionDecider,
                                                                                              StateAttributeDecider<R> stateAttributeDecider)
    {
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        throw new UnsupportedOperationException(Misc.NIY);
    }

    @Override
    public <S extends Symbol> FSA<S> makeCompleteDelegated(FSA<S> target)
    {
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> minimizeDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        throw new UnsupportedOperationException(Misc.NIY);
    }

    @Override
    public <S extends Symbol> FSA<S> makeComplementDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return getDecoratee().makeComplement(determinizeDelegated(target));
    }
}
