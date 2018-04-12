package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface MutableAutomaton<S> extends Automaton<S>
{
    @Override
    MutableState<S> startState();

    @Override
    default Automaton<S> trimEpsilonTransitions()
    {
        final var epsilon = alphabet().epsilon();
        final var statesWithEpsilonTrans = states().selectWith(State::transitionExists, epsilon);
        final Queue<MutableState<S>> pendingChecks = new LinkedList<>();
        statesWithEpsilonTrans.forEach(state -> pendingChecks.add((MutableState<S>) state));

        final List<State<S>> clearedStates = new LinkedList<>();
        var workSize = statesWithEpsilonTrans.size() + 1;
        while (!pendingChecks.isEmpty() && pendingChecks.size() < workSize) {
            workSize = pendingChecks.size();
            for (var i = 0; i < workSize; i++) {
                final var currState = pendingChecks.poll();
                clearedStates.clear();
                assert currState != null;
                for (var succ : currState.successors(epsilon)) {
                    if (succ.transitionExists(epsilon)) {
                        pendingChecks.add(currState);
                        continue;
                    }
                    currState.addTransitions(succ.transitions());
                    clearedStates.add(succ);
                }
                clearedStates.forEach(cleared -> currState.removeTransition(epsilon, (MutableState<S>) cleared));
            }
        }
        if (!pendingChecks.isEmpty()) {
            throw new UnsupportedOperationException("circulation handling " + NOT_IMPLEMENTED_YET);
        }

        return this; // in-place reference
    }

    default <R> Automaton<R> projectInto(MutableAutomaton<R> result, Function<S, R> projector)
    {
        final MutableMap<State<S>, MutableState<R>> stateMapping = UnifiedMap.newMap(states().size());
        stateMapping.put(startState(), result.startState());

        R newSymbol;
        for (var dept : states()) {
            final var newDept = stateMapping.computeIfAbsent(dept, __ -> result.newState());
            for (var symbolAndDest : dept.transitions()) {
                final var symbol = symbolAndDest.getOne();
                final var dest = symbolAndDest.getTwo();
                if ((newSymbol = projector.apply(symbol)) != null) {
                    final var newDest = stateMapping.computeIfAbsent(dest, __ -> result.newState());
                    result.addTransition(newDept, newDest, newSymbol);
                }
            }
        }
        acceptStates().forEach(originalAccept -> result.setAsAccept(stateMapping.get(originalAccept)));

        return result.trimUnreachableStates(); // one-off
    }

    @Override
    TransitionGraph<S> transitionGraph();

    @Override
    default MutableAutomaton<S> toMutable()
    {
        return this;
    }

    MutableAutomaton<S> setAlphabet(Alphabet<S> alphabet);

    default MutableAutomaton<S> addSymbol(S symbol)
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    MutableState<S> newState();

    default MutableState<S> newState(String name)
    {
        return newState().setName(name);
    }

    MutableAutomaton<S> addState(MutableState<S> state);

    default MutableAutomaton<S> addStates(RichIterable<MutableState<S>> states)
    {
        states.forEach(this::addState);

        return this;
    }

    MutableAutomaton<S> removeState(MutableState<S> state);

    default MutableAutomaton<S> removeStates(RichIterable<MutableState<S>> states)
    {
        states.forEach(this::removeState);

        return this;
    }

    MutableAutomaton<S> setAsStart(MutableState<S> state);

    MutableAutomaton<S> setAsAccept(MutableState<S> state);

    MutableAutomaton<S> unsetAccept(MutableState<S> state);

    default MutableAutomaton<S> setAllAsAccept(RichIterable<MutableState<S>> states)
    {
        states.forEach(this::setAsAccept);

        return this;
    }

    MutableAutomaton<S> resetAcceptStates();

    MutableAutomaton<S> addTransition(MutableState<S> dept, MutableState<S> dest, S symbol);

    default MutableAutomaton<S> addEpsilonTransition(MutableState<S> dept, MutableState<S> dest)
    {
        return addTransition(dept, dest, alphabet().epsilon());
    }

    @Override
    String toString();

    interface TransitionGraph<S> extends Automaton.TransitionGraph<S>
    {
        @Override
        String toString();
    }
}
