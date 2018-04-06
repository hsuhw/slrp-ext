package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.function.Function;

import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface MutableAutomaton<S> extends Automaton<S>
{
    @Override
    MutableState<S> startState();

    default <R> Automaton<R> projectInto(MutableAutomaton<R> result, Function<S, R> projector)
    {
        final MutableMap<State<S>, MutableState<R>> stateMapping = UnifiedMap.newMap(states().size());
        stateMapping.put(startState(), result.startState());

        R newSymbol;
        for (var dept : states()) {
            final var newDept = stateMapping.computeIfAbsent(dept, __ -> result.newState());
            for (var symbol : dept.enabledSymbols()) {
                for (var dest : dept.successors(symbol)) {
                    if ((newSymbol = projector.apply(symbol)) != null) {
                        final var newDest = stateMapping.computeIfAbsent(dest, __ -> result.newState());
                        result.addTransition(newDept, newDest, newSymbol);
                    }
                }
            }
        }
        acceptStates().forEach(originAccept -> result.setAsAccept(stateMapping.get(originAccept)));

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
