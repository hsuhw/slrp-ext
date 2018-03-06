package core.automata;

import api.automata.*;
import common.util.Assert;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.LinkedList;
import java.util.Queue;

import static api.util.Constants.NONEXISTING_STATE;

public abstract class AbstractMutableAutomaton<S> implements MutableAutomaton<S>
{
    protected final Alphabet<S> alphabet;
    protected final MutableSet<State<S>> states;
    protected final MutableSet<State<S>> acceptStates;
    private State<S> startState;

    public AbstractMutableAutomaton(Alphabet<S> alphabet, int stateCapacity)
    {
        this.alphabet = alphabet;
        states = UnifiedSet.newSet(stateCapacity);
        acceptStates = UnifiedSet.newSet(stateCapacity);
        startState = newState();
    }

    public AbstractMutableAutomaton(AbstractMutableAutomaton<S> toCopy, boolean deep)
    {
        if (deep) {
            final int stateSize = toCopy.states.size();
            final MutableMap<State<S>, MutableState<S>> stateMapping = UnifiedMap.newMap(stateSize);

            alphabet = toCopy.alphabet;
            states = UnifiedSet.newSet(stateSize);
            startState = newState();

            toCopy.states.forEach(stateToCopy -> {
                final MutableState<S> newState = stateMapping.computeIfAbsent(stateToCopy, __ -> newState());
                stateToCopy.enabledSymbols().forEach(symbol -> stateToCopy.successors(symbol).forEach(succ -> {
                    final MutableState<S> newSucc = stateMapping.computeIfAbsent(succ, __ -> newState());
                    newState.addTransition(symbol, newSucc);
                }));
            });
            acceptStates = toCopy.acceptStates.collect(stateMapping::get);
        } else {
            alphabet = toCopy.alphabet;
            states = UnifiedSet.newSet(toCopy.states);
            acceptStates = UnifiedSet.newSet(toCopy.acceptStates);
            startState = toCopy.startState;
        }
    }

    @Override
    public Alphabet<S> alphabet()
    {
        return alphabet;
    }

    @Override
    public SetIterable<State<S>> states()
    {
        return states.asUnmodifiable();
    }

    @Override
    public State<S> startState()
    {
        return startState;
    }

    @Override
    public SetIterable<State<S>> acceptStates()
    {
        return acceptStates.asUnmodifiable();
    }

    @Override
    public SetIterable<State<S>> nonAcceptStates()
    {
        return states.difference(acceptStates);
    }

    @Override
    public SetIterable<State<S>> liveStates()
    {
        final MutableSet<State<S>> result = UnifiedSet.newSet(states().size()); // upper bound
        final MapIterable<State<S>, SetIterable<State<S>>> predecessors = predecessorRelation();
        final Queue<State<S>> pendingChecks = new LinkedList<>(acceptStates);

        State<S> currLiving;
        while ((currLiving = pendingChecks.poll()) != null) {
            predecessors.get(currLiving).forEach(alsoLiving -> {
                if (result.add(alsoLiving)) {
                    pendingChecks.add(alsoLiving);
                }
            });
        }

        return result;
    }

    @Override
    public MutableAutomaton.TransitionGraph<S> transitionGraph()
    {
        return new TransitionGraphView();
    }

    protected abstract MutableState<S> createState();

    @Override
    public MutableState<S> newState()
    {
        final MutableState<S> state = createState();
        states.add(state);

        return state;
    }

    @Override
    public MutableAutomaton<S> addState(MutableState<S> state)
    {
        Assert.argumentNotNull(state);

        states.add(state);

        return this;
    }

    @Override
    public MutableAutomaton<S> removeState(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }
        if (state == startState) {
            throw new IllegalArgumentException("start state cannot be removed");
        }

        states.remove(state);
        states.forEach(affected -> ((MutableState<S>) affected).removeTransitionsTo(state));

        return this;
    }

    @Override
    public MutableAutomaton<S> setAsStart(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        startState = state;

        return this;
    }

    @Override
    public MutableAutomaton<S> setAsAccept(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        acceptStates.add(state);

        return this;
    }

    @Override
    public MutableAutomaton<S> unsetAccept(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        acceptStates.remove(state);

        return this;
    }

    @Override
    public MutableAutomaton<S> resetAcceptStates()
    {
        acceptStates.clear();

        return this;
    }

    @Override
    public MutableAutomaton<S> addTransition(MutableState<S> dept, MutableState<S> dest, S symbol)
    {
        if (!states.containsAllArguments(dept, dest)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        dept.addTransition(symbol, dest);

        return this;
    }

    @Override
    public String toString()
    {
        return toString("", "");
    }

    private class TransitionGraphView implements MutableAutomaton.TransitionGraph<S>
    {
        @Override
        public Automaton<S> automaton()
        {
            return AbstractMutableAutomaton.this;
        }
    }
}
