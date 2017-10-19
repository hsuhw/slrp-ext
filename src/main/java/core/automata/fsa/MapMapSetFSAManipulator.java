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

import static api.util.Values.NOT_IMPLEMENTED_YET;

public class MapMapSetFSAManipulator implements FSAManipulator.Decorator
{
    private final FSAManipulator decoratee;

    public MapMapSetFSAManipulator(FSAManipulator decoratee)
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

    private <S> FSA<S> trim(FSA<S> targetFSA, ImmutableSet<State> toBeTrimmed)
    {
        final FSA.Builder<S> builder = FSAs.builderOn(targetFSA);
        toBeTrimmed.forEach(builder::removeState);

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
        final int stateNumber = target.states().size();
        final MutableSet<State> reachable = UnifiedSet.newSet(stateNumber); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();
        final ImmutableSet<State> startStates = fsa.startStates();
        reachable.addAllIterable(startStates);
        pendingChecks.addAll(startStates.castToSet());
        computeStateReachability(fsa.transitionGraph()::successorsOf, reachable, pendingChecks);

        return reachable.size() == stateNumber ? fsa : trim(fsa, target.states().newWithoutAll(reachable));
    }

    @Override
    public <S> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target)
    {
        if (!isFSA(target)) {
            return null;
        }

        // mark all the reachable states by a backward BFS
        final FSA<S> fsa = (FSA<S>) target;
        final int stateNumber = target.states().size();
        final MutableSet<State> reachable = UnifiedSet.newSet(stateNumber); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();
        final ImmutableSet<State> acceptStates = fsa.acceptStates();
        reachable.addAllIterable(acceptStates);
        pendingChecks.addAll(acceptStates.castToSet());
        computeStateReachability(fsa.transitionGraph()::predecessorsOf, reachable, pendingChecks);

        return reachable.size() == stateNumber ? fsa : trim(fsa, target.states().newWithoutAll(reachable));
    }

    @Override
    public <S, T, R> FSA<R> makeProductDelegated(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                                 BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
    {
        if (!isFSA(one) || !isFSA(two)) {
            return null;
        }

        final FSA<S> fsa1 = (FSA<S>) one;
        final FSA<T> fsa2 = (FSA<T>) two;
        final int size = fsa1.states().size() * fsa2.states().size(); // upper bound
        final ProductDeltaBuilder<S, T, R> deltaBuilder = new ProductDeltaBuilder<>(size, fsa1, fsa2);
        final MapMapSetFSABuilder<R> builder = new MapMapSetFSABuilder<>(size, alphabet.size(), alphabet.epsilon());
        finalizer.apply(deltaBuilder.run(builder, transitionDecider), builder);

        return builder.acceptStates().isEmpty() ? FSAs.thatAcceptsNone(alphabet) : builder.build();
    }

    @Override
    public <S> FSA<S> determinizeDelegated(FSA<S> target)
    {
        if (!(target instanceof MapMapSetFSA<?>)) {
            return null;
        }
        if (target.isDeterministic()) {
            return target;
        }

        final Alphabet<S> alphabet = target.alphabet();
        final TransitionGraph<State, S> delta = target.transitionGraph();
        final int capacity = target.states().size() * target.states().size(); // heuristic
        final FSA.Builder<S> builder = FSAs.builder(capacity, alphabet.size(), alphabet.epsilon());
        final MutableBiMap<MutableSet<State>, State> stateMapping = new HashBiMap<>(capacity);
        final Queue<MutableSet<State>> pendingStateSets = new LinkedList<>();

        final MutableSet<State> startStates = delta.epsilonClosureOf(target.startStates()).toSet();
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

    private class ProductDeltaBuilder<S, T, R>
    {
        final FSA<S> fsa1;
        final FSA<T> fsa2;
        final TransitionGraph<State, S> delta1;
        final TransitionGraph<State, T> delta2;
        final S epsilon1;
        final T epsilon2;
        final MutableBiMap<Twin<State>, State> stateMapping;
        final Queue<Twin<State>> pendingProductStates;

        private ProductDeltaBuilder(int capacity, FSA<S> one, FSA<T> two)
        {
            fsa1 = one;
            fsa2 = two;
            delta1 = one.transitionGraph();
            delta2 = two.transitionGraph();
            epsilon1 = delta1.epsilonLabel();
            epsilon2 = delta2.epsilonLabel();
            stateMapping = new HashBiMap<>(capacity);
            pendingProductStates = new LinkedList<>();
        }

        private State getState(Twin<State> statePair)
        {
            return stateMapping.computeIfAbsent(statePair, pair -> {
                pendingProductStates.add(pair);
                return States.generate();
            });
        }

        private State getState(State state1, State state2)
        {
            return getState(Tuples.twin(state1, state2));
        }

        private void handleEpsilonTransitions(FSA.Builder<R> carrier, Twin<State> statePair, R epsilonP)
        {
            final State deptP = getState(statePair);
            final State dept1 = statePair.getOne();
            final State dept2 = statePair.getTwo();
            delta1.successorsOf(dept1, epsilon1).forEach(dest1 -> {
                carrier.addTransition(deptP, getState(dest1, dept2), epsilonP);
            });
            delta2.successorsOf(dept2, epsilon2).forEach(dest2 -> {
                carrier.addTransition(deptP, getState(dept1, dest2), epsilonP);
            });
        }

        private MutableBiMap<Twin<State>, State> run(FSA.Builder<R> carrier, BiFunction<S, T, R> transitionDecider)
        {
            final R epsilonP = transitionDecider.apply(epsilon1, epsilon2);

            // FIXME: currently only single start state FSAs can be handled
            final Twin<State> startStatePair = Tuples.twin(fsa1.startState(), fsa2.startState());
            stateMapping.put(startStatePair, States.generate());
            pendingProductStates.add(startStatePair);
            Twin<State> currStatePair;
            while ((currStatePair = pendingProductStates.poll()) != null) {
                final State deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(carrier, currStatePair, epsilonP);
                final State dept1 = currStatePair.getOne();
                final State dept2 = currStatePair.getTwo();
                for (S symbol1 : delta1.nonEpsilonArcsOn(dept1)) {
                    for (T symbolB : delta2.nonEpsilonArcsOn(dept2)) {
                        final R symbolP = transitionDecider.apply(symbol1, symbolB);
                        if (symbolP == null) {
                            continue;
                        }
                        delta1.successorsOf(dept1, symbol1).forEach(dest1 -> {
                            delta2.successorsOf(dept2, symbolB).forEach(dest2 -> {
                                carrier.addTransition(deptP, getState(dest1, dest2), symbolP);
                            });
                        });
                    }
                }
            }

            return stateMapping;
        }
    }
}
