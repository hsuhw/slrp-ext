package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import core.automata.DoubleMapDelta;
import core.automata.States;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.function.BiFunction;

public class DoubleMapDFSAManipulator implements FSAManipulator.Decorator
{
    private final FSAManipulator decoratee;

    public DoubleMapDFSAManipulator(FSAManipulator decoratee)
    {
        this.decoratee = decoratee;
    }

    @Override
    public FSAManipulator getDecoratee()
    {
        return decoratee;
    }

    private <S extends Symbol> boolean isImplementationTarget(Automaton<S> target)
    {
        return target instanceof DoubleMapDFSA<?>;
    }

    @Override
    public <S extends Symbol> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }

    @Override
    public <S extends Symbol, T extends Symbol, R extends Symbol> FSA<S> composeDelegated(Automaton<S> first,
                                                                                          Automaton<T> after,
                                                                                          BiFunction<S, T, R> composer)
    {
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return target;
    }

    private <S extends Symbol> MutableList<State> collectIncompleteStates(Alphabet<S> alphabet,
                                                                          MutableList<State> states,
                                                                          TransitionFunction<S> transes)
    {
        final MutableList<State> incompleteStates = FastList.newList(states.size()); // upper bound
        final int completeSize = alphabet.size() - 1; // without epsilon
        for (State s : states) {
            if (transes.enabledSymbolsOn(s).size() != completeSize) {
                incompleteStates.add(s);
            }
        }
        return incompleteStates;
    }

    private <S extends Symbol> DoubleMapDelta<S> addDeadEndStateToDelta(ImmutableSet<S> alphabetSet,
                                                                        MutableList<State> states,
                                                                        DoubleMapDelta<S> transes,
                                                                        MutableList<State> incompleteStates)
    {
        final MutableMap<State, MutableMap<S, State>> delta = transes.getMutableDefinition();
        final MutableMap<State, MutableMap<S, MutableSet<State>>> deltaInversed = transes
            .getMutableInversedDefinition();

        // create the dead end state
        final State deadEndState = States.generateOne();
        states.add(deadEndState);
        delta.put(deadEndState, UnifiedMap.newMap(alphabetSet.size()));
        deltaInversed.put(deadEndState, UnifiedMap.newMap(alphabetSet.size()));
        final MutableMap<S, State> deadEndStateSuccs = delta.get(deadEndState);
        alphabetSet.forEach(symbol -> deadEndStateSuccs.put(symbol, deadEndState));
        final MutableMap<S, MutableSet<State>> deadEndStatePreds = deltaInversed.get(deadEndState);
        alphabetSet.forEach(symbol -> {
            deadEndStatePreds.getIfAbsentPut(symbol, UnifiedSet.newSet(states.size())) // upper bound
                             .add(deadEndState);
        });

        // make incomplete states complete
        incompleteStates.forEach(state -> {
            alphabetSet.forEach(symbol -> {
                delta.get(state).putIfAbsent(symbol, deadEndState);
                deadEndStatePreds.get(symbol).add(state);
            });
        });

        return new DoubleMapDelta<>(delta, deltaInversed);
    }

    @Override
    public <S extends Symbol> DoubleMapDFSA<S> makeCompleteDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        final DoubleMapDFSA<S> targetDFSA = (DoubleMapDFSA<S>) target;
        final Alphabet<S> alphabet = targetDFSA.getAlphabet();
        final MutableList<State> states = targetDFSA.getStates().toList();
        final DoubleMapDelta<S> transes = targetDFSA.getTransitionFunction();
        final MutableList<State> incompleteStates = collectIncompleteStates(alphabet, states, transes);
        if (incompleteStates.isEmpty()) {
            return targetDFSA;
        }
        final DoubleMapDelta<S> delta = addDeadEndStateToDelta(alphabet.toSet(), states, transes, incompleteStates);
        final MutableBooleanList startStateTable = targetDFSA.getStartStateTable().toList();
        startStateTable.add(false); // the dead end state is not a start state
        final MutableBooleanList acceptStateTable = targetDFSA.getAcceptStateTable().toList();
        acceptStateTable.add(false); // the dead end state is not an accept state

        return new DoubleMapDFSA<>(alphabet, states, startStateTable, acceptStateTable, delta);
    }

    @Override
    public <S extends Symbol> DoubleMapDFSA<S> minimizeDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }

    @Override
    public <S extends Symbol> DoubleMapDFSA<S> getComplementDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        final DoubleMapDFSA<S> targetDFSA = makeCompleteDelegated(target);
        final MutableBooleanList acceptStateTable = targetDFSA.getAcceptStateTable().toList();
        for (int i = 0; i < acceptStateTable.size(); i++) {
            acceptStateTable.set(i, !acceptStateTable.get(i));
        }
        return new DoubleMapDFSA<>(targetDFSA.getAlphabet(), targetDFSA.getStates(), targetDFSA.getStartStateTable(),
                                   acceptStateTable.toImmutable(), targetDFSA.getTransitionFunction());
    }
}
