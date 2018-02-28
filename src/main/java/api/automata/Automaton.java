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
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import static api.util.Constants.NONEXISTING_STATE;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface Automaton<S extends State<T>, T>
{
    Alphabet<T> alphabet();

    SetIterable<S> states();

    S startState();

    SetIterable<S> acceptStates();

    SetIterable<S> nonAcceptStates();

    default boolean isAcceptState(State<T> state)
    {
        return acceptStates().contains(state);
    }

    default SetIterable<S> reachableStates()
    {
        final MutableSet<S> result = UnifiedSet.newSet(states().size()); // upper bound
        result.add(startState());
        final Queue<S> pendingChecks = new LinkedList<>();
        pendingChecks.add(startState());

        S currReached;
        while ((currReached = pendingChecks.poll()) != null) {
            currReached.successors().forEach(thenReached -> {
                @SuppressWarnings("unchecked")
                final S laterReachedCasted = (S) thenReached;
                if (result.add(laterReachedCasted)) {
                    pendingChecks.add(laterReachedCasted);
                }
            });
        }

        return result;
    }

    default SetIterable<S> unreachableStates()
    {
        return states().difference(reachableStates());
    }

    default MapIterable<S, SetIterable<S>> predecessorRelation()
    {
        final MutableMap<S, MutableSet<S>> result = UnifiedMap.newMap(states().size());
        states().forEach(dept -> dept.successors().forEach(dest -> {
            @SuppressWarnings("unchecked")
            final S destCasted = (S) dest;
            result.computeIfAbsent(destCasted, __ -> UnifiedSet.newSet()).add(dept);
        }));

        return result.collect((key, value) -> Tuples.pair(key, (SetIterable<S>) value));
    }

    default SetIterable<S> liveStates()
    {
        final MutableSet<S> result = UnifiedSet.newSet(states().size()); // upper bound
        final MapIterable<S, SetIterable<S>> predecessors = predecessorRelation();
        @SuppressWarnings("unchecked")
        final Set<S> acceptStates = (Set<S>) acceptStates();
        final Queue<S> pendingChecks = new LinkedList<>(acceptStates);

        S currLiving;
        while ((currLiving = pendingChecks.poll()) != null) {
            predecessors.get(currLiving).forEach(alsoLiving -> {
                if (result.add(alsoLiving)) {
                    pendingChecks.add(alsoLiving);
                }
            });
        }

        return result;
    }

    default SetIterable<S> deadEndStates()
    {
        return states().difference(liveStates());
    }

    default SetIterable<S> danglingStates()
    {
        return states().difference(reachableStates().intersect(liveStates()));
    }

    Automaton<? extends State<T>, T> trimUnreachableStates();

    <R> Automaton<? extends State<R>, R> project(Alphabet<R> alphabet, Function<T, R> projector);

    <U extends State<V>, V, R> Automaton<? extends State<R>, R> product(Automaton<U, V> target, Alphabet<R> alphabet,
        StepMaker<S, T, U, V, R> stepMaker, Finalizer<S, U, MutableState<R>, R> finalizer);

    boolean isDeterministic();

    TransitionGraph<S, T> transitionGraph();

    MutableAutomaton<? extends MutableState<T>, T> toMutable();

    default ImmutableAutomaton<? extends ImmutableState<T>, T> toImmutable()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    /**
     * This method only make sense when there is no possible contravariance
     * introduced in this interface.
     */
    static <S extends State<T>, T> Automaton<State<T>, T> upcast(Automaton<S, T> derivative)
    {
        @SuppressWarnings("unchecked")
        final Automaton<State<T>, T> generalized = (Automaton<State<T>, T>) derivative;
        return generalized;
    }

    @Override
    String toString();

    String toString(String indent, String nameTag);


    @FunctionalInterface
    interface StepMaker<S extends State<T>, T, U extends State<V>, V, R>
    {
        R apply(Pair<S, U> deptPair, T symbol1, V symbol2);
    }

    @FunctionalInterface
    interface Finalizer<S, T, U extends MutableState<R>, R>
    {
        void apply(BiMap<Pair<S, T>, U> stateMapping, MutableAutomaton<U, R> builder);
    }

    interface TransitionGraph<N extends State<A>, A> extends Digraph<N, A>
    {
        Automaton<N, A> automaton();

        @Override
        default SetIterable<N> referredNodes()
        {
            return automaton().states();
        }

        @Override
        default boolean nodeExists(Object node)
        {
            return automaton().states().contains(node);
        }

        @Override
        default SetIterable<A> referredArcLabels()
        {
            return automaton().alphabet().asSet();
        }

        @Override
        default RichIterable<? extends Pair<A, N>> arcsFrom(N node)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.transitions().collect(each -> {
                @SuppressWarnings("unchecked")
                final N State = (N) each.getTwo();
                return Tuples.pair(each.getOne(), State);
            });
        }

        @Override
        default SetIterable<A> arcLabelsFrom(N node)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.enabledSymbols();
        }

        @Override
        default boolean arcLabeledFrom(N node, A arcLabel)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.transitionExists(arcLabel);
        }

        @Override
        default RichIterable<Pair<N, A>> arcsTo(N node)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default SetIterable<A> arcLabelsTo(N node)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default boolean arcLabeledTo(N node, A arcLabel)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default SetIterable<A> arcLabelsOn(N from, N to)
        {
            if (!automaton().states().containsAllArguments(from, to)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return from.enabledSymbolsTo(to);
        }

        @Override
        default SetIterable<N> directSuccessorsOf(N node)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.successors().collect(each -> {
                @SuppressWarnings("unchecked")
                final N succ = (N) each;
                return succ;
            }).toSet();
        }

        @Override
        default SetIterable<N> directSuccessorsOf(N node, A arcLabel)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            return node.successors(arcLabel).collect(each -> {
                @SuppressWarnings("unchecked")
                final N succ = (N) each;
                return succ;
            }).toSet();
        }

        @Override
        default SetIterable<N> directPredecessorsOf(N node)
        {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        @Override
        default SetIterable<N> directPredecessorsOf(N node, A arcLabel)
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

        default A epsilonLabel()
        {
            return automaton().alphabet().epsilon();
        }

        default SetIterable<A> nonEpsilonArcLabelsFrom(N node)
        {
            return arcLabelsFrom(node).difference(Sets.immutable.of(epsilonLabel()));
        }

        default N directSuccessorOf(N node, A arcLabel)
        {
            if (!nodeExists(node)) {
                throw new IllegalArgumentException(NONEXISTING_STATE);
            }

            @SuppressWarnings("unchecked")
            final N succ = (N) node.successor(arcLabel);
            return succ;
        }

        default SetIterable<N> epsilonClosureOf(N node)
        {
            return epsilonClosureOf(Sets.immutable.of(node));
        }

        default SetIterable<N> epsilonClosureOf(SetIterable<N> nodes)
        {
            return epsilonClosureOf(nodes, epsilonLabel());
        }

        default SetIterable<N> epsilonClosureOf(SetIterable<N> nodes, A arcLabel)
        {
            MutableSet<N> base = UnifiedSet.newSet(referredNodes().size()); // upper bound
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
