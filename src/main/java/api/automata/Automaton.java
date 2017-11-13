package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

public interface Automaton<S>
{
    Alphabet<S> alphabet();

    ImmutableSet<State> states();

    ImmutableSet<State> startStates();

    default State startState()
    {
        if (startStates().size() != 1) {
            throw new UnsupportedOperationException("more than one start states");
        }

        return startStates().getOnly();
    }

    default boolean isStartState(State state)
    {
        return startStates().contains(state);
    }

    default ImmutableSet<State> nonStartStates()
    {
        return states().newWithoutAll(startStates());
    }

    ImmutableSet<State> acceptStates();

    default boolean isAcceptState(State state)
    {
        return acceptStates().contains(state);
    }

    default ImmutableSet<State> nonAcceptStates()
    {
        return states().newWithoutAll(acceptStates());
    }

    default SetIterable<State> reachableStatesWith(ImmutableSet<State> base,
                                                   Function<State, SetIterable<State>> stepFunction)
    {
        final MutableSet<State> reachable = UnifiedSet.newSet(states().size()); // upper bound
        reachable.addAllIterable(base);
        final Queue<State> pendingChecks = new LinkedList<>();
        pendingChecks.addAll(base.castToSet());

        State currState;
        while ((currState = pendingChecks.poll()) != null) {
            stepFunction.apply(currState).forEach(state -> {
                if (reachable.add(state)) {
                    pendingChecks.add(state);
                }
            });
        }

        return reachable; // one-off
    }

    default ImmutableSet<State> unreachableStates()
    {
        return states().newWithoutAll(reachableStatesWith(startStates(), transitionGraph()::successorsOf));
    }

    default ImmutableSet<State> deadEndStates()
    {
        return states().newWithoutAll(reachableStatesWith(acceptStates(), transitionGraph()::predecessorsOf));
    }

    default SetIterable<State> danglingStates()
    {
        return Sets.union(unreachableStates().castToSet(), deadEndStates().castToSet()); // one-off
    }

    TransitionGraph<State, S> transitionGraph();

    @Override
    String toString();

    String toString(boolean maskStateNames);

    interface Builder<S>
    {
        Builder<S> addSymbol(S symbol);

        Builder<S> addState(State state);

        Builder<S> removeState(State state);

        Builder<S> addStartState(State state);

        Builder<S> addStartStates(ImmutableSet<State> states);

        Builder<S> resetStartStates();

        Builder<S> addAcceptState(State state);

        Builder<S> addAcceptStates(ImmutableSet<State> states);

        Builder<S> resetAcceptStates();

        Builder<S> addTransition(State dept, State dest, S symbol);

        Automaton<S> build();
    }
}
