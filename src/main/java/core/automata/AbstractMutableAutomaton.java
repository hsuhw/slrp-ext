package core.automata;

import api.automata.*;
import common.util.Assert;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

import static api.util.Constants.NONEXISTING_STATE;
import static core.Parameters.estimateExtendedSize;

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
        final int capacity = estimateExtendedSize(toCopy.states.size());
        if (deep) {
            final MutableMap<State<S>, MutableState<S>> stateMapping = UnifiedMap.newMap(capacity);

            alphabet = toCopy.alphabet;
            states = UnifiedSet.newSet(capacity);
            startState = newState();

            toCopy.states.forEach(stateToCopy -> {
                final MutableState<S> newState = stateMapping.computeIfAbsent(stateToCopy, __ -> newState());
                stateToCopy.enabledSymbols().forEach(symbol -> stateToCopy.successors(symbol).forEach(succ -> {
                    final MutableState<S> newSucc = stateMapping.computeIfAbsent(succ, __ -> newState());
                    newState.addTransition(symbol, newSucc);
                }));
            });
            acceptStates = UnifiedSet.newSet(toCopy.states.size());
            toCopy.acceptStates.forEach(state -> acceptStates.add(stateMapping.get(state)));
        } else {
            alphabet = toCopy.alphabet;
            states = UnifiedSet.newSet(capacity);
            states.addAllIterable(toCopy.states);
            acceptStates = UnifiedSet.newSet(toCopy.states.size());
            acceptStates.addAllIterable(toCopy.acceptStates);
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


    protected class ProductHandler<T, R>
    {
        private final Automaton<T> target;
        private final S epsilon1;
        private final T epsilon2;
        private final MutableAutomaton<R> result;
        private final MutableBiMap<Pair<State<S>, State<T>>, MutableState<R>> stateMapping;
        private final Queue<Pair<State<S>, State<T>>> pendingChecks;

        public ProductHandler(Automaton<T> target, MutableAutomaton<R> result, int capacity)
        {
            this.target = target;
            epsilon1 = alphabet().epsilon();
            epsilon2 = target.alphabet().epsilon();
            this.result = result;
            stateMapping = new HashBiMap<>(capacity);
            pendingChecks = new LinkedList<>();
        }

        private MutableState<R> takeState(Pair<State<S>, State<T>> statePair)
        {
            return stateMapping.computeIfAbsent(statePair, pair -> {
                pendingChecks.add(pair);
                return result.newState();
            });
        }

        private MutableState<R> takeState(State<S> one, State<T> two)
        {
            return takeState(Tuples.pair(one, two));
        }

        private void handleEpsilonTransitions(Pair<State<S>, State<T>> statePair)
        {
            final MutableState<R> deptP = takeState(statePair);
            final State<S> dept1 = statePair.getOne();
            final State<T> dept2 = statePair.getTwo();
            dept1.successors(epsilon1).forEach(dest -> result.addEpsilonTransition(deptP, takeState(dest, dept2)));
            dept2.successors(epsilon2).forEach(dest -> result.addEpsilonTransition(deptP, takeState(dept1, dest)));
        }

        public ProductHandler<T, R> makeProduct(StepMaker<S, T, R> stepMaker)
        {
            takeState(startState(), target.startState());
            Pair<State<S>, State<T>> currStatePair;
            while ((currStatePair = pendingChecks.poll()) != null) {
                final MutableState<R> deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(currStatePair);
                final State<S> dept1 = currStatePair.getOne();
                final State<T> dept2 = currStatePair.getTwo();
                for (S symbol1 : dept1.enabledSymbols()) {
                    if (symbol1.equals(epsilon1)) {
                        continue; // already handled
                    }
                    for (T symbol2 : dept2.enabledSymbols()) {
                        if (symbol2.equals(epsilon2)) {
                            continue; // already handled
                        }
                        final R symbolP = stepMaker.apply(currStatePair, symbol1, symbol2);
                        if (symbolP == null) {
                            continue; // no step should be made
                        }
                        dept1.successors(symbol1).forEach(dest1 -> dept2.successors(symbol2).forEach(dest2 -> {
                            result.addTransition(deptP, takeState(dest1, dest2), symbolP);
                        }));
                    }
                }
            }

            return this;
        }

        public MutableAutomaton<R> settle(Finalizer<S, T, R> finalizer)
        {
            finalizer.apply(stateMapping, result);

            return result;
        }
    }
}
