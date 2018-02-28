package core.automata;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.MutableAutomaton;
import api.automata.MutableState;
import common.util.Assert;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.util.Constants.NONEXISTING_STATE;

public abstract class AbstractMutableAutomaton<S extends MutableState<T>, T> implements MutableAutomaton<S, T>
{
    protected final Alphabet<T> alphabet;
    protected final MutableSet<S> states;
    protected final MutableSet<S> acceptStates;
    private S startState;

    public AbstractMutableAutomaton(Alphabet<T> alphabet, int stateCapacity)
    {
        this.alphabet = alphabet;
        states = UnifiedSet.newSet(stateCapacity);
        acceptStates = UnifiedSet.newSet(stateCapacity);
        startState = newState();
    }

    public AbstractMutableAutomaton(AbstractMutableAutomaton<S, T> toBeCopied, boolean deep)
    {
        if (deep) {
            final int stateSize = toBeCopied.states.size();
            final MutableMap<S, S> stateMapping = UnifiedMap.newMap(stateSize);

            alphabet = toBeCopied.alphabet;
            states = UnifiedSet.newSet(stateSize);
            startState = newState();

            toBeCopied.states.forEach(stateToCopy -> {
                final S newState = stateMapping.computeIfAbsent(stateToCopy, __ -> newState());
                stateToCopy.enabledSymbols().forEach(symbol -> stateToCopy.successors(symbol).forEach(succ -> {
                    @SuppressWarnings("unchecked")
                    final S succCasted = (S) succ;
                    final S newSucc = stateMapping.computeIfAbsent(succCasted, __ -> newState());
                    newState.addTransition(symbol, newSucc);
                }));
            });
            acceptStates = toBeCopied.acceptStates.collect(stateMapping::get);
        } else {
            alphabet = toBeCopied.alphabet;
            states = UnifiedSet.newSet(toBeCopied.states);
            acceptStates = UnifiedSet.newSet(toBeCopied.acceptStates);
            startState = toBeCopied.startState;
        }
    }

    @Override
    public Alphabet<T> alphabet()
    {
        return alphabet;
    }

    @Override
    public SetIterable<S> states()
    {
        return states.asUnmodifiable();
    }

    @Override
    public S startState()
    {
        return startState;
    }

    @Override
    public SetIterable<S> acceptStates()
    {
        return acceptStates.asUnmodifiable();
    }

    @Override
    public SetIterable<S> nonAcceptStates()
    {
        return states.difference(acceptStates);
    }

    @Override
    public MutableAutomaton.TransitionGraph<S, T> transitionGraph()
    {
        return new TransitionGraphView();
    }

    protected abstract S createState();

    @Override
    public S newState()
    {
        final S state = createState();
        states.add(state);

        return state;
    }

    @Override
    public MutableAutomaton<S, T> addState(S state)
    {
        Assert.argumentNotNull(state);

        states.add(state);

        return this;
    }

    @Override
    public MutableAutomaton<S, T> removeState(S state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }
        if (state == startState) {
            throw new IllegalArgumentException("start state cannot be removed");
        }

        states.remove(state);
        states.forEach(affected -> affected.removeTransitionsTo(state));

        return this;
    }

    @Override
    public MutableAutomaton<S, T> setAsStart(S state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        startState = state;

        return this;
    }

    @Override
    public MutableAutomaton<S, T> setAsAccept(S state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        acceptStates.add(state);

        return this;
    }

    @Override
    public MutableAutomaton<S, T> unsetAccept(S state)
    {
        if (!states.contains(state)) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        acceptStates.remove(state);

        return this;
    }

    @Override
    public MutableAutomaton<S, T> resetAcceptStates()
    {
        acceptStates.clear();

        return this;
    }

    @Override
    public MutableAutomaton<S, T> addTransition(S dept, S dest, T symbol)
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

    private class TransitionGraphView implements MutableAutomaton.TransitionGraph<S, T>
    {
        @Override
        public Automaton<S, T> automaton()
        {
            return AbstractMutableAutomaton.this;
        }
    }
}
