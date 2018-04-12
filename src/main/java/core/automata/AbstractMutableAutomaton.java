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
    private final MutableSet<State<S>> states;
    private final MutableSet<State<S>> acceptStates;
    private final TransitionGraph transitionGraph;
    private Alphabet<S> alphabet;
    private MutableState<S> startState;

    protected boolean hasChanged;
    private MapIterable<State<S>, String> stateNames;
    private SetIterable<State<S>> nonAcceptStates;
    private SetIterable<State<S>> reachableStates;
    private SetIterable<State<S>> unreachableStates;
    private MapIterable<State<S>, SetIterable<State<S>>> predecessorRelation;
    private SetIterable<State<S>> liveStates;
    private SetIterable<State<S>> deadEndStates;
    private SetIterable<State<S>> danglingStates;

    public AbstractMutableAutomaton(Alphabet<S> alphabet, int stateCapacity)
    {
        this.alphabet = alphabet;
        states = UnifiedSet.newSet(stateCapacity);
        acceptStates = UnifiedSet.newSet(stateCapacity);
        transitionGraph = new TransitionGraph();
        startState = newState();
    }

    public AbstractMutableAutomaton(AbstractMutableAutomaton<S> toCopy, boolean deep)
    {
        final var capacity = estimateExtendedSize(toCopy.states.size());
        if (deep) {
            final MutableMap<State<S>, MutableState<S>> stateMapping = UnifiedMap.newMap(capacity);

            alphabet = toCopy.alphabet;
            states = UnifiedSet.newSet(capacity);
            transitionGraph = new TransitionGraph();
            startState = newState();
            stateMapping.put(toCopy.startState, startState);

            toCopy.states.forEach(stateToCopy -> {
                final var newState = stateMapping.computeIfAbsent(stateToCopy, __ -> newState());
                stateToCopy.transitions().forEach(symbolAndDest -> {
                    final var newSucc = stateMapping.computeIfAbsent(symbolAndDest.getTwo(), __ -> newState());
                    newState.addTransition(symbolAndDest.getOne(), newSucc);
                });
            });
            acceptStates = UnifiedSet.newSet(toCopy.states.size());
            toCopy.acceptStates.forEach(state -> acceptStates.add(stateMapping.get(state)));
        } else {
            alphabet = toCopy.alphabet;
            states = UnifiedSet.newSet(capacity);
            states.addAllIterable(toCopy.states);
            transitionGraph = new TransitionGraph();
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
    public MapIterable<State<S>, String> stateNames()
    {
        if (!hasChanged && stateNames != null) {
            return stateNames;
        }

        return (stateNames = MutableAutomaton.super.stateNames());
    }

    @Override
    public MutableState<S> startState()
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
        if (!hasChanged && nonAcceptStates != null) {
            return nonAcceptStates;
        }

        return (nonAcceptStates = states.difference(acceptStates));
    }

    @Override
    public SetIterable<State<S>> reachableStates()
    {
        if (!hasChanged && reachableStates != null) {
            return reachableStates;
        }

        return (reachableStates = MutableAutomaton.super.reachableStates());
    }

    @Override
    public SetIterable<State<S>> unreachableStates()
    {
        if (!hasChanged && unreachableStates != null) {
            return unreachableStates;
        }

        return (unreachableStates = MutableAutomaton.super.unreachableStates());
    }

    @Override
    public MapIterable<State<S>, SetIterable<State<S>>> predecessorRelation()
    {
        if (!hasChanged && predecessorRelation != null) {
            return predecessorRelation;
        }

        return (predecessorRelation = MutableAutomaton.super.predecessorRelation());
    }

    @Override
    public SetIterable<State<S>> liveStates()
    {
        if (!hasChanged && liveStates != null) {
            return liveStates;
        }

        return (liveStates = MutableAutomaton.super.liveStates());
    }

    @Override
    public SetIterable<State<S>> deadEndStates()
    {
        if (!hasChanged && deadEndStates != null) {
            return deadEndStates;
        }

        return (deadEndStates = MutableAutomaton.super.deadEndStates());
    }

    @Override
    public SetIterable<State<S>> danglingStates()
    {
        if (!hasChanged && danglingStates != null) {
            return danglingStates;
        }

        return (danglingStates = MutableAutomaton.super.danglingStates());
    }

    @Override
    public MutableAutomaton.TransitionGraph<S> transitionGraph()
    {
        return transitionGraph;
    }

    @Override
    public MutableAutomaton<S> setAlphabet(Alphabet<S> alphabet)
    {
        final var givenAlphabet = alphabet.asSet();
        if (!states.allSatisfy(that -> givenAlphabet.containsAllIterable(that.enabledSymbols()))) {
            throw new IllegalArgumentException("given alphabet does not contain all used symbols");
        }

        this.alphabet = alphabet;
        hasChanged = true;

        return this;
    }

    protected abstract MutableState<S> createState();

    @Override
    public MutableState<S> newState()
    {
        final var state = createState();
        states.add(state);
        hasChanged = true;

        return state;
    }

    @Override
    public MutableAutomaton<S> addState(MutableState<S> state)
    {
        Assert.argumentNotNull(state);

        states.add(state);
        hasChanged = true;

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
        hasChanged = true;

        return this;
    }

    @Override
    public MutableAutomaton<S> setAsStart(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        startState = state;
        hasChanged = true;

        return this;
    }

    @Override
    public MutableAutomaton<S> setAsAccept(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        acceptStates.add(state);
        hasChanged = true;

        return this;
    }

    @Override
    public MutableAutomaton<S> unsetAccept(MutableState<S> state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        acceptStates.remove(state);
        hasChanged = true;

        return this;
    }

    @Override
    public MutableAutomaton<S> resetAcceptStates()
    {
        acceptStates.clear();
        hasChanged = true;

        return this;
    }

    @Override
    public MutableAutomaton<S> addTransition(MutableState<S> dept, MutableState<S> dest, S symbol)
    {
        if (!states.containsAllArguments(dept, dest)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        dept.addTransition(symbol, dest);
        hasChanged = true;

        return this;
    }

    private class TransitionGraph implements MutableAutomaton.TransitionGraph<S>
    {
        int size = -1;
        SetIterable<S> referredArcLabels;

        @Override
        public Automaton<S> automaton()
        {
            return AbstractMutableAutomaton.this;
        }

        @Override
        public SetIterable<S> referredArcLabels()
        {
            if (!hasChanged && referredArcLabels != null) {
                return referredArcLabels;
            }

            return (referredArcLabels = MutableAutomaton.TransitionGraph.super.referredArcLabels());
        }

        @Override
        public int size()
        {
            if (!hasChanged && size != -1) {
                return size;
            }

            return (size = MutableAutomaton.TransitionGraph.super.size());
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

        public ProductHandler<T, R> makeProduct(StepMaker<S, T, R> stepMaker)
        {
            final var dummyStart = result.startState();
            result.setAsStart(takeState(startState(), target.startState()));
            result.removeState(dummyStart);
            Pair<State<S>, State<T>> currStatePair;
            while ((currStatePair = pendingChecks.poll()) != null) {
                final var deptP = stateMapping.get(currStatePair);
                final var dept1 = currStatePair.getOne();
                final var dept2 = currStatePair.getTwo();
                dept1.successors(epsilon1).forEach(dest -> result.addEpsilonTransition(deptP, takeState(dest, dept2)));
                dept2.successors(epsilon2).forEach(dest -> result.addEpsilonTransition(deptP, takeState(dept1, dest)));
                for (var symbol1 : dept1.enabledSymbols()) {
                    if (symbol1.equals(epsilon1)) {
                        continue; // already handled
                    }
                    for (var symbol2 : dept2.enabledSymbols()) {
                        if (symbol2.equals(epsilon2)) {
                            continue; // already handled
                        }
                        final var symbolP = stepMaker.apply(currStatePair, symbol1, symbol2);
                        if (symbolP == null) {
                            continue; // no step should be made
                        }
                        dept1.successors(symbol1).forEach(dest1 -> dept2.successors(symbol2).forEach(
                            dest2 -> result.addTransition(deptP, takeState(dest1, dest2), symbolP)));
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
