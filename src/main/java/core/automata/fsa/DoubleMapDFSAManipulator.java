package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAManipulatorDecorator;
import core.automata.DoubleMapDelta;
import core.automata.States;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.BiMaps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiFunction;

public class DoubleMapDFSAManipulator implements FSAManipulatorDecorator
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

    private <S extends Symbol, T extends Symbol, R extends Symbol> DoubleMapDelta<R> computeProductDelta(
        DoubleMapDFSA<S> dfsaA, DoubleMapDFSA<T> dfsaB, MutableBiMap<State, Twin<State>> stateMapping,
        BiFunction<S, T, R> transitionDecider)
    {
        final TransitionFunction<S> deltaA = dfsaA.getTransitionFunction();
        final TransitionFunction<T> deltaB = dfsaB.getTransitionFunction();
        final int symbolNumberUpperBound = dfsaA.getAlphabetSize() * dfsaB.getAlphabetSize();
        final Queue<Twin<State>> pendingProductStates = new LinkedList<>();
        final MutableMap<State, MutableMap<R, State>> newDeltaDefinition = UnifiedMap
            .newMap(dfsaA.getStateNumber() * dfsaB.getStateNumber()); // upper bound

        final Twin<State> startStatePair = Tuples.twin(dfsaA.getStartState(), dfsaB.getStartState());
        stateMapping.put(States.generateOne(), startStatePair);
        pendingProductStates.add(startStatePair);
        Twin<State> currStatePair;
        while ((currStatePair = pendingProductStates.poll()) != null) {
            final State prodDept = stateMapping.inverse().get(currStatePair);
            final State deptA = currStatePair.getOne();
            final State deptB = currStatePair.getTwo();
            deltaA.enabledSymbolsOn(deptA).forEach(symbolA -> {
                deltaB.enabledSymbolsOn(deptB).forEach(symbolB -> {
                    final R prodTransSymbol = transitionDecider.apply(symbolA, symbolB);
                    if (prodTransSymbol != null) {
                        final State destA = deltaA.successorOf(deptA, symbolA);
                        final State destB = deltaB.successorOf(deptB, symbolB);
                        final Twin<State> destStatePair = Tuples.twin(destA, destB);
                        if (!stateMapping.containsValue(destStatePair)) {
                            stateMapping.put(States.generateOne(), destStatePair);
                            pendingProductStates.add(destStatePair);
                        }
                        State prodDest = stateMapping.inverse().get(destStatePair);
                        newDeltaDefinition.getIfAbsentPut(prodDept, UnifiedMap.newMap(symbolNumberUpperBound))
                                          .putIfAbsent(prodTransSymbol, prodDest);
                    }
                });
            });
        }

        return new DoubleMapDelta<>(newDeltaDefinition);
    }

    @Override
    public <S extends Symbol, T extends Symbol, R extends Symbol> DoubleMapDFSA<R> makeProductDelegated(
        Automaton<S> one, Automaton<T> two, Alphabet<R> targetAlphabet, BiFunction<S, T, R> transitionDecider,
        StateAttributeDecider<R> stateAttributeDecider)
    {
        if (!isImplementationTarget(one) || !isImplementationTarget(two)) {
            return null;
        }
        final DoubleMapDFSA<S> dfsaA = (DoubleMapDFSA<S>) one;
        final DoubleMapDFSA<T> dfsaB = (DoubleMapDFSA<T>) two;
        final MutableBiMap<State, Twin<State>> stateMapping = BiMaps.mutable.empty();
        final DoubleMapDelta<R> newDelta = computeProductDelta(dfsaA, dfsaB, stateMapping, transitionDecider);
        final StateAttributes stateAttributes = stateAttributeDecider.decide(stateMapping, newDelta);

        return new DoubleMapDFSA<>(targetAlphabet, stateAttributes.getDefinitionOfStates(),
                                   stateAttributes.getStartStateTable(), stateAttributes.getAcceptStateTable(),
                                   newDelta);
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

        // create a dead end state
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

        // add the ignored dead end transitions of those incomplete states back
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

        // collect the incomplete states
        final DoubleMapDFSA<S> targetDFSA = (DoubleMapDFSA<S>) target;
        final Alphabet<S> alphabet = targetDFSA.getAlphabet();
        final MutableList<State> states = targetDFSA.getStates().toList();
        final DoubleMapDelta<S> transes = targetDFSA.getTransitionFunction();
        final MutableList<State> incompleteStates = collectIncompleteStates(alphabet, states, transes);
        if (incompleteStates.isEmpty()) {
            return targetDFSA;
        }

        // complete the ignored transitions of those states
        final ImmutableSet<S> noEpsilonAlphabet = alphabet.toSet().newWithout(alphabet.getEpsilonSymbol());
        final DoubleMapDelta<S> delta = addDeadEndStateToDelta(noEpsilonAlphabet, states, transes, incompleteStates);
        final MutableBooleanList startStateTable = targetDFSA.getStartStateTable().toList();
        startStateTable.add(false); // the dead end state shouldn't be a start state
        final MutableBooleanList acceptStateTable = targetDFSA.getAcceptStateTable().toList();
        acceptStateTable.add(false); // the dead end state shouldn't be an accept state

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
    public <S extends Symbol> DoubleMapDFSA<S> makeComplementDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        final DoubleMapDFSA<S> targetDFSA = makeCompleteDelegated(target);
        final ImmutableBooleanList originalAcceptStateTable = targetDFSA.getAcceptStateTable();
        final ImmutableBooleanList acceptStateTableComplement = makeAcceptStateComplement(originalAcceptStateTable);
        return new DoubleMapDFSA<>(targetDFSA.getAlphabet(), targetDFSA.getStates(), targetDFSA.getStartStateTable(),
                                   acceptStateTableComplement, targetDFSA.getTransitionFunction());
    }
}
