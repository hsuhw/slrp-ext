package api.automata;

import common.Digraph;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

import static api.util.Constants.NONEXISTING_STATE;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface Automaton<S>
{
    Alphabet<S> alphabet();

    SetIterable<State<S>> states();

    State<S> startState();

    SetIterable<State<S>> acceptStates();

    SetIterable<State<S>> nonAcceptStates();

    default boolean isAcceptState(State<S> state)
    {
        return acceptStates().contains(state);
    }

    default SetIterable<State<S>> reachableStates()
    {
        final MutableSet<State<S>> result = UnifiedSet.newSet(states().size()); // upper bound
        result.add(startState());
        final Queue<State<S>> pendingChecks = new LinkedList<>();
        pendingChecks.add(startState());

        State<S> currReached;
        while ((currReached = pendingChecks.poll()) != null) {
            currReached.successors().forEach(thenReached -> {
                if (result.add(thenReached)) {
                    pendingChecks.add(thenReached);
                }
            });
        }

        return result;
    }

    default SetIterable<State<S>> unreachableStates()
    {
        return states().difference(reachableStates());
    }

    default MapIterable<State<S>, SetIterable<State<S>>> predecessorRelation()
    {
        final MutableMap<State<S>, MutableSet<State<S>>> result = UnifiedMap.newMap(states().size());
        states().forEach(dept -> dept.successors().forEach(dest -> {
            result.computeIfAbsent(dest, __ -> UnifiedSet.newSet()).add(dept);
        }));

        @SuppressWarnings("unchecked")
        final MutableMap<State<S>, SetIterable<State<S>>> resultCasted = (MutableMap) result;
        return resultCasted;
    }

    SetIterable<State<S>> liveStates();

    default SetIterable<State<S>> deadEndStates()
    {
        return states().difference(liveStates());
    }

    default SetIterable<State<S>> danglingStates()
    {
        return states().difference(reachableStates().intersect(liveStates()));
    }

    Automaton<S> trimUnreachableStates();

    <R> Automaton<R> project(Alphabet<R> alphabet, Function<S, R> projector);

    <T, R> Automaton<R> product(Automaton<T> target, Alphabet<R> alphabet, StepMaker<S, T, R> stepMaker,
        Finalizer<S, T, R> finalizer);

    boolean isDeterministic();

    TransitionGraph<S> transitionGraph();

    MutableAutomaton<S> toMutable();

    default ImmutableAutomaton<S> toImmutable()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    String toString();

    String toString(String indent, String nameTag);

    @FunctionalInterface
    interface StepMaker<S, T, R>
    {
        R apply(Pair<State<S>, State<T>> statePair, S symbol1, T symbol2);
    }

    @FunctionalInterface
    interface Finalizer<S, T, R>
    {
        void apply(BiMap<Pair<State<S>, State<T>>, MutableState<R>> stateMapping, MutableAutomaton<R> builder);
    }

    interface TransitionGraph<S> extends Digraph<State<S>, S>
    {

        Automaton<S> automaton();

        @Override
        default SetIterable<State<S>> referredNodes()
        {
            return automaton().states();
        }

        @Override
        default boolean nodeExists(State<S> node)
        {
            return automaton().states().contains(node);
        }

        @Override
        default SetIterable<S> referredArcLabels()
        {
            return automaton().alphabet().asSet();
        }

        @Override
        default RichIterable<Pair<S, State<S>>> arcsFrom(State<S> node)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.transitions();
        }

        @Override
        default SetIterable<S> arcLabelsFrom(State<S> node)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.enabledSymbols();
        }

        @Override
        default boolean arcLabeledFrom(State<S> node, S arcLabel)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.transitionExists(arcLabel);
        }

        @Override
        default RichIterable<Pair<State<S>, S>> arcsTo(State<S> node)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default SetIterable<S> arcLabelsTo(State<S> node)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default boolean arcLabeledTo(State<S> node, S arcLabel)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default SetIterable<S> arcLabelsOn(State<S> from, State<S> to)
        {
            if (!automaton().states().containsAllArguments(from, to)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return from.enabledSymbolsTo(to);
        }

        @Override
        default SetIterable<State<S>> directSuccessorsOf(State<S> node)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.successors();
        }

        @Override
        default SetIterable<State<S>> directSuccessorsOf(State<S> node, S arcLabel)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.successors(arcLabel);
        }

        @Override
        default SetIterable<State<S>> directPredecessorsOf(State<S> node)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default SetIterable<State<S>> directPredecessorsOf(State<S> node, S arcLabel)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        default int size()
        {
            return (int) automaton().states().sumOfLong(
                state -> state.enabledSymbols().sumOfInt(transLabel -> state.successors(transLabel).size()));
        }

        default boolean isEmpty()
        {
            return size() == 0;
        }

        default S epsilonLabel()
        {
            return automaton().alphabet().epsilon();
        }

        default SetIterable<S> nonEpsilonArcLabelsFrom(State<S> node)
        {
            return arcLabelsFrom(node).difference(Sets.immutable.of(epsilonLabel()));
        }

        default State<S> directSuccessorOf(State<S> node, S arcLabel)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.successor(arcLabel);
        }

        default SetIterable<State<S>> epsilonClosureOf(State<S> node)
        {
            return epsilonClosureOf(Sets.immutable.of(node));
        }

        default SetIterable<State<S>> epsilonClosureOf(SetIterable<State<S>> nodes)
        {
            return epsilonClosureOf(nodes, epsilonLabel());
        }

        default SetIterable<State<S>> epsilonClosureOf(SetIterable<State<S>> nodes, S arcLabel)
        {
            MutableSet<State<S>> base = UnifiedSet.newSet(referredNodes().size()); // upper bound
            base.addAllIterable(nodes);
            while (true) {
                if (!base.addAllIterable(directSuccessorsOf(base, epsilonLabel()))) {
                    break; // `base` has converged
                }
            }

            return arcLabel.equals(epsilonLabel()) ? base : epsilonClosureOf(directSuccessorsOf(base, arcLabel));
        }

        @Override
        String toString();
    }
}
