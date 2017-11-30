package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

import static api.automata.fsa.FSA.Builder;
import static api.automata.fsa.FSAs.builder;

public class BasicFSAManipulator implements FSAManipulator.Decorator
{
    private final FSAManipulator decoratee;

    public BasicFSAManipulator(FSAManipulator decoratee)
    {
        this.decoratee = decoratee;
    }

    @Override
    public FSAManipulator decoratee()
    {
        return decoratee;
    }

    @Override
    public <S, T, R> FSA<R> productImpl(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                        SymbolDecider<S, T, R> symbolDecider, Finalizer<R> finalizer)
    {
        if (!isFSA(one) || !isFSA(two)) {
            return null;
        }

        final ProductBuilder<S, T, R> builder = new ProductBuilder<>(alphabet, (FSA<S>) one, (FSA<T>) two);

        return builder.build(symbolDecider, finalizer);
    }

    @Override
    public <S, T, R> FSA<R> productImpl(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                        StepFilter<S, T, R> stepFilter, Finalizer<R> finalizer)
    {
        if (!isFSA(one) || !isFSA(two)) {
            return null;
        }

        final ProductBuilder<S, T, R> builder = new ProductBuilder<>(alphabet, (FSA<S>) one, (FSA<T>) two);

        return builder.build(stepFilter, finalizer);
    }

    @Override
    public <S> LanguageSubsetChecker.Result<S> checkSubsetImpl(FSA<S> subsumer, FSA<S> includer)
    {
        return LanguageSubsetCheckerSingleton.INSTANCE.test(subsumer, includer);
    }

    private class ProductBuilder<S, T, R>
    {
        private final FSA<S> fsa1;
        private final FSA<T> fsa2;
        private final TransitionGraph<State, S> delta1;
        private final TransitionGraph<State, T> delta2;
        private final S epsilon1;
        private final T epsilon2;
        private final R epsilonP;
        private final Alphabet<R> alphabet;
        private final Builder<R> builder;
        private final MutableBiMap<Twin<State>, State> stateMapping;
        private final Queue<Twin<State>> pendingProductStates;

        private ProductBuilder(Alphabet<R> alphabet, FSA<S> one, FSA<T> two)
        {
            final int capacity = one.states().size() * two.states().size(); // upper bound
            fsa1 = one;
            fsa2 = two;
            delta1 = one.transitionGraph();
            delta2 = two.transitionGraph();
            epsilon1 = delta1.epsilonLabel();
            epsilon2 = delta2.epsilonLabel();
            epsilonP = alphabet.epsilon();
            this.alphabet = alphabet;
            builder = builder(capacity, alphabet.size(), alphabet.epsilon());
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

        private void handleEpsilonTransitions(Twin<State> statePair)
        {
            final State deptP = getState(statePair);
            final State dept1 = statePair.getOne();
            final State dept2 = statePair.getTwo();
            delta1.directSuccessorsOf(dept1, epsilon1).forEach(dest1 -> {
                builder.addTransition(deptP, getState(dest1, dept2), epsilonP);
            });
            delta2.directSuccessorsOf(dept2, epsilon2).forEach(dest2 -> {
                builder.addTransition(deptP, getState(dept1, dest2), epsilonP);
            });
        }

        private void makeProduct(SymbolDecider<S, T, R> symbolDecider)
        {
            // FIXME: currently only handles the single start state FSAs
            final Twin<State> startStatePair = Tuples.twin(fsa1.startState(), fsa2.startState());
            stateMapping.put(startStatePair, States.generate());
            pendingProductStates.add(startStatePair);
            Twin<State> currStatePair;
            while ((currStatePair = pendingProductStates.poll()) != null) {
                final State deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(currStatePair);
                final State dept1 = currStatePair.getOne();
                final State dept2 = currStatePair.getTwo();
                for (S symbol1 : delta1.nonEpsilonArcLabelsFrom(dept1)) {
                    for (T symbol2 : delta2.nonEpsilonArcLabelsFrom(dept2)) {
                        final R symbolP = symbolDecider.apply(symbol1, symbol2);
                        if (symbolP == null) {
                            continue;
                        }
                        delta1.directSuccessorsOf(dept1, symbol1).forEach(dest1 -> {
                            delta2.directSuccessorsOf(dept2, symbol2).forEach(dest2 -> {
                                builder.addTransition(deptP, getState(dest1, dest2), symbolP);
                            });
                        });
                    }
                }
            }
        }

        private FSA<R> settle(Finalizer<R> finalizer)
        {
            finalizer.apply(stateMapping, builder);

            return builder.currentAcceptStateNumber() == 0
                   ? FSAs.thatAcceptsNone(alphabet)
                   : FSAs.trimDanglingStates(builder.buildWith(alphabet));
        }

        private FSA<R> build(SymbolDecider<S, T, R> symbolDecider, Finalizer<R> finalizer)
        {
            makeProduct(symbolDecider);

            return settle(finalizer);
        }

        private void makeProduct(StepFilter<S, T, R> stepFilter)
        {
            // FIXME: currently only handles the single start state FSAs
            final Twin<State> startStatePair = Tuples.twin(fsa1.startState(), fsa2.startState());
            stateMapping.put(startStatePair, States.generate());
            pendingProductStates.add(startStatePair);
            Twin<State> currStatePair;
            while ((currStatePair = pendingProductStates.poll()) != null) {
                final State deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(currStatePair);
                final State dept1 = currStatePair.getOne();
                final State dept2 = currStatePair.getTwo();
                for (S symbol1 : delta1.nonEpsilonArcLabelsFrom(dept1)) {
                    for (T symbol2 : delta2.nonEpsilonArcLabelsFrom(dept2)) {
                        final R symbolP = stepFilter.apply(currStatePair, symbol1, symbol2);
                        if (symbolP == null) {
                            continue;
                        }
                        delta1.directSuccessorsOf(dept1, symbol1).forEach(dest1 -> {
                            delta2.directSuccessorsOf(dept2, symbol2).forEach(dest2 -> {
                                builder.addTransition(deptP, getState(dest1, dest2), symbolP);
                            });
                        });
                    }
                }
            }
        }

        private FSA<R> build(StepFilter<S, T, R> stepFilter, Finalizer<R> finalizer)
        {
            makeProduct(stepFilter);

            return settle(finalizer);
        }
    }

    private static final class LanguageSubsetCheckerSingleton
    {
        private static final LanguageSubsetChecker INSTANCE = new BasicLanguageSubsetChecker();
    }
}
