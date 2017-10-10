package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAs;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static api.util.Values.NOT_IMPLEMENTED_YET;

public class BasicFSAManipulator implements FSAManipulator.Decorator
{
    private final FSAManipulator decoratee;

    public BasicFSAManipulator(FSAManipulator decoratee)
    {
        this.decoratee = decoratee;
    }

    @Override
    public FSAManipulator getDecoratee()
    {
        return decoratee;
    }

    private <S> boolean isFSA(Automaton<S> target)
    {
        return target instanceof FSA<?>;
    }

    private void prepareStateReachabilitySearch(SetIterable<State> states, Predicate<State> initialCondition,
                                                MutableSet<State> reachableStates, Queue<State> pendingChecks)
    {
        states.forEach(state -> {
            if (initialCondition.test(state)) {
                reachableStates.add(state);
                pendingChecks.add(state);
            }
        });
    }

    private void computeStateReachability(Function<State, SetIterable<State>> stepFunction,
                                          MutableSet<State> reachableStates, Queue<State> pendingChecks)
    {
        State currState;
        while ((currState = pendingChecks.poll()) != null) {
            stepFunction.apply(currState).forEach(state -> {
                if (!reachableStates.contains(state)) {
                    reachableStates.add(state);
                    pendingChecks.add(state);
                }
            });
        }
    }

    private <S> FSA<S> trimStates(FSA<S> targetFSA, ImmutableSet<State> trimmingSet)
    {
        final FSA.Builder<S> builder = FSAs.builderBasedOn(targetFSA);
        trimmingSet.forEach(builder::removeState);

        return builder.build();
    }

    @Override
    public <S> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target)
    {
        if (!isFSA(target)) {
            return null;
        }

        // mark all the reachable states by a forward BFS
        final FSA<S> fsa = (FSA<S>) target;
        final int stateNumber = target.getStateNumber();
        final ImmutableSet<State> states = target.getStates();
        final State startState = fsa.getStartState();
        final MutableSet<State> reachable = UnifiedSet.newSet(stateNumber); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();
        reachable.add(startState);
        pendingChecks.add(startState);
        computeStateReachability(fsa.getDeltaFunction()::successorsOf, reachable, pendingChecks);

        return reachable.size() == stateNumber ? fsa : trimStates(fsa, states.newWithoutAll(reachable));
    }

    @Override
    public <S> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target)
    {
        if (!isFSA(target)) {
            return null;
        }

        // mark all the reachable states by a backward BFS
        final FSA<S> fsa = (FSA<S>) target;
        final int stateNumber = target.getStateNumber();
        final ImmutableSet<State> states = target.getStates();
        final MutableSet<State> reachable = UnifiedSet.newSet(stateNumber); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();
        prepareStateReachabilitySearch(states, target::isAcceptState, reachable, pendingChecks);
        computeStateReachability(fsa.getDeltaFunction()::predecessorsOf, reachable, pendingChecks);

        return reachable.size() == stateNumber ? fsa : trimStates(fsa, states.newWithoutAll(reachable));
    }

    private <S> void addProductTransition(Queue<Twin<State>> pendingProductStates,
                                          MutableBiMap<Twin<State>, State> stateMapping, FSA.Builder<S> builder,
                                          State deptP, Twin<State> destStatePair, S symbolP)
    {
        final State destP = stateMapping.computeIfAbsent(destStatePair, __ -> {
            pendingProductStates.add(destStatePair);
            return States.generate();
        });
        builder.addTransition(deptP, destP, symbolP);
    }

    private <S, T, R> void computeProductDelta(FSA<S> fsaA, FSA<T> fsaB, MutableBiMap<Twin<State>, State> stateMapping,
                                               FSA.Builder<R> builder, BiFunction<S, T, R> transitionDecider)
    {
        final DeltaFunction<S> deltaA = fsaA.getDeltaFunction();
        final DeltaFunction<T> deltaB = fsaB.getDeltaFunction();
        final S epsilonA = deltaA.getEpsilonSymbol();
        final T epsilonB = deltaB.getEpsilonSymbol();
        final R epsilonP = transitionDecider.apply(epsilonA, epsilonB);
        final Queue<Twin<State>> pendingProductStates = new LinkedList<>();

        final Twin<State> startStatePair = Tuples.twin(fsaA.getStartState(), fsaB.getStartState());
        stateMapping.put(startStatePair, States.generate());
        pendingProductStates.add(startStatePair);
        Twin<State> currStatePair;
        while ((currStatePair = pendingProductStates.poll()) != null) {
            final State deptP = stateMapping.get(currStatePair);
            final State deptA = currStatePair.getOne();
            final State deptB = currStatePair.getTwo();
            for (S symbolA : deltaA.enabledSymbolsOn(deptA)) {
                if (symbolA.equals(epsilonA)) {
                    deltaA.successorsOf(deptA, epsilonA).forEach(destA -> {
                        addProductTransition(pendingProductStates, stateMapping, builder, deptP,
                                             Tuples.twin(destA, deptB), epsilonP);
                    });
                    continue;
                }
                for (T symbolB : deltaB.enabledSymbolsOn(deptB)) {
                    if (symbolB.equals(epsilonB)) {
                        deltaB.successorsOf(deptB, epsilonB).forEach(destB -> {
                            addProductTransition(pendingProductStates, stateMapping, builder, deptP,
                                                 Tuples.twin(deptA, destB), epsilonP);
                        });
                        continue;
                    }
                    final R symbolP = transitionDecider.apply(symbolA, symbolB);
                    if (symbolP != null) {
                        deltaA.successorsOf(deptA, symbolA).forEach(destA -> {
                            deltaB.successorsOf(deptB, symbolB).forEach(destB -> {
                                addProductTransition(pendingProductStates, stateMapping, builder, deptP,
                                                     Tuples.twin(destA, destB), symbolP);
                            });
                        });
                    }
                }
            }
        }
    }

    @Override
    public <S, T, R> FSA<R> makeProductDelegated(Automaton<S> one, Automaton<T> two, Alphabet<R> targetAlphabet,
                                                 BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
    {
        if (!isFSA(one) || !isFSA(two)) {
            return null;
        }

        final FSA<S> fsaA = (FSA<S>) one;
        final FSA<T> fsaB = (FSA<T>) two;
        final int stateNumberEstimate = fsaA.getStateNumber() * fsaB.getStateNumber(); // upper bound
        final MutableBiMap<Twin<State>, State> stateMapping = new HashBiMap<>(stateNumberEstimate);
        final FSA.Builder<R> builder = FSAs
            .builder(targetAlphabet.size(), targetAlphabet.epsilon(), stateNumberEstimate);

        computeProductDelta(fsaA, fsaB, stateMapping, builder, transitionDecider);
        finalizer.apply(stateMapping, builder);

        try {
            return builder.build();
        } catch (IllegalStateException e) {
            return FSAs.withEmptyLanguage(targetAlphabet);
        }
    }

    @Override
    public <S> FSA<S> determinizeDelegated(FSA<S> target)
    {
        if (target instanceof MapMapDFSA<?>) {
            return target;
        }
        if (!(target instanceof MapMapSetNFSA<?>)) {
            return null;
        }

        final Alphabet<S> alphabet = target.getAlphabet();
        final DeltaFunction<S> delta = target.getDeltaFunction();
        final int statePowerSetEstimate = target.getStateNumber() * target.getStateNumber(); // heuristic
        final FSA.Builder<S> builder = FSAs
            .builder(alphabet.size(), alphabet.epsilon(), statePowerSetEstimate);
        final MutableBiMap<MutableSet<State>, State> stateMapping = new HashBiMap<>(statePowerSetEstimate);
        final Queue<MutableSet<State>> pendingStateSets = new LinkedList<>();

        final MutableSet<State> startStates = delta.epsilonClosureOf(target.getStartStates()).toSet();
        final State newStart = States.generate();
        stateMapping.put(startStates, newStart);
        builder.addStartState(newStart);
        pendingStateSets.add(startStates);
        final SetIterable<S> noEpsilonSymbolSet = alphabet.noEpsilonSet();
        MutableSet<State> currStateSet;
        while ((currStateSet = pendingStateSets.poll()) != null) {
            final State newDept = stateMapping.get(currStateSet);
            for (S symbol : noEpsilonSymbolSet) {
                final MutableSet<State> destStates = delta.epsilonClosureOf(currStateSet, symbol).toSet();
                final State newDest = stateMapping.computeIfAbsent(destStates, __ -> {
                    pendingStateSets.add(destStates);
                    final State s = States.generate();
                    if (destStates.anySatisfy(target::isAcceptState)) {
                        builder.addAcceptState(s);
                    }
                    return s;
                });
                builder.addTransition(newDept, newDest, symbol);
            }
        }

        return builder.build(alphabet);
    }

    @Override
    public <S> FSA<S> minimizeDelegated(FSA<S> target)
    {
        if (!isFSA(target)) {
            return null;
        }

        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }
}
