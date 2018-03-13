package api.automata.fst;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.MutableFSA;
import api.util.Connectives;
import core.automata.fst.BasicMutableFST;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.function.Function;

import static api.util.Connectives.AND;
import static api.util.Constants.NONEXISTING_STATE;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface MutableFST<S, T> extends MutableAutomaton<Pair<S, T>>, FST<S, T>
{
    @Override
    default FST<S, T> trimUnreachableStates()
    {
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<Pair<S, T>>> unreachableStates = (SetIterable) unreachableStates();
        return FSTs.deepCopy(this).removeStates(unreachableStates);
    }

    private <R> Automaton<R> projectInto(MutableAutomaton<R> result, Function<Pair<S, T>, R> projector)
    {
        final MutableMap<State<Pair<S, T>>, MutableState<R>> stateMapping = UnifiedMap.newMap(states().size());
        stateMapping.put(startState(), (MutableState<R>) result.startState());

        R newSymbol;
        for (State<Pair<S, T>> dept : states()) {
            final MutableState<R> newDept = stateMapping.computeIfAbsent(dept, __ -> result.newState());
            for (Pair<S, T> symbol : dept.enabledSymbols()) {
                for (State<Pair<S, T>> dest : dept.successors(symbol)) {
                    if ((newSymbol = projector.apply(symbol)) != null) {
                        final MutableState<R> newDest = stateMapping.computeIfAbsent(dest, __ -> result.newState());
                        result.addTransition(newDept, newDest, newSymbol);
                    }
                }
            }
        }
        acceptStates().forEach(originAccept -> result.setAsAccept(stateMapping.get(originAccept)));

        return result.trimUnreachableStates(); // one-off
    }

    @Override
    default <R> Automaton<R> project(Alphabet<R> alphabet, Function<Pair<S, T>, R> projector)
    {
        final int capacity = states().size(); // upper bound
        if (alphabet.epsilon() instanceof Pair<?, ?>) {
            @SuppressWarnings("unchecked")
            final MutableAutomaton<R> result = new BasicMutableFST(alphabet, capacity);
            return projectInto(result, projector);
        } else {
            final MutableFSA<R> result = FSAs.create(alphabet, capacity);
            return projectInto(result, projector);
        }
    }

    @Override
    default MutableFST<S, T> toMutable()
    {
        return this;
    }

    @Override
    default MutableFST<S, T> addSymbol(Pair<S, T> symbol)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.addSymbol(symbol);
    }

    @Override
    MutableFST<S, T> setAlphabet(Alphabet<Pair<S, T>> alphabet);

    @Override
    MutableFST<S, T> addState(MutableState<Pair<S, T>> state);

    @Override
    default MutableFST<S, T> addStates(RichIterable<MutableState<Pair<S, T>>> states)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.addStates(states);
    }

    @Override
    MutableFST<S, T> removeState(MutableState<Pair<S, T>> state);

    @Override
    default MutableFST<S, T> removeStates(RichIterable<MutableState<Pair<S, T>>> states)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.removeStates(states);
    }

    @Override
    MutableFST<S, T> setAsStart(MutableState<Pair<S, T>> state);

    @Override
    MutableFST<S, T> setAsAccept(MutableState<Pair<S, T>> state);

    @Override
    MutableFST<S, T> unsetAccept(MutableState<Pair<S, T>> state);

    @Override
    default MutableFST<S, T> setAllAsAccept(RichIterable<MutableState<Pair<S, T>>> states)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.setAllAsAccept(states);
    }

    @Override
    MutableFST<S, T> resetAcceptStates();

    @Override
    MutableFST<S, T> addTransition(MutableState<Pair<S, T>> dept, MutableState<Pair<S, T>> dest, Pair<S, T> symbol);

    @Override
    default MutableFST<S, T> addEpsilonTransition(MutableState<Pair<S, T>> dept, MutableState<Pair<S, T>> dest)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.addEpsilonTransition(dept, dest);
    }

    @Override
    default FST<S, T> intersect(FST<S, T> target)
    {
        return (FST<S, T>) product(target, alphabet(), Connectives.Labels.matched(),
                                   Connectives.AcceptStates.select(this, target, AND)); // one-off
    }

    @Override
    default FST<S, T> union(FST<S, T> target)
    {
        if (!(target instanceof MutableFST<?, ?>) || !alphabet().equals(target.alphabet())) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }
        if (!states().containsAllIterable(target.states())) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        final MutableFST<S, T> result = FSTs.shallowCopy(this);
        final MutableState<Pair<S, T>> newStart = result.newState();
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<Pair<S, T>>> targetStates = (SetIterable) target.states();
        result.addEpsilonTransition(newStart, (MutableState<Pair<S, T>>) startState()).setAsStart(newStart)
              .addStates(targetStates).addEpsilonTransition(newStart, (MutableState<Pair<S, T>>) target.startState());

        return result; // shallow reference
    }

    @Override
    default FSA<Pair<S, T>> asFSA()
    {
        return FSAs.castFrom(this);
    }
}
