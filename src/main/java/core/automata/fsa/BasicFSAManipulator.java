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
import java.util.function.BiFunction;

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
                                        BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
    {
        if (!isFSA(one) || !isFSA(two)) {
            return null;
        }

        final FSA<S> fsa1 = (FSA<S>) one;
        final FSA<T> fsa2 = (FSA<T>) two;
        final int size = fsa1.states().size() * fsa2.states().size(); // upper bound
        final ProductDeltaBuilder<S, T, R> deltaBuilder = new ProductDeltaBuilder<>(size, fsa1, fsa2);
        final Builder<R> builder = builder(size, alphabet.size(), alphabet.epsilon());
        finalizer.apply(deltaBuilder.run(builder, transitionDecider), builder);

        return builder.currentAcceptStateNumber() == 0
               ? FSAs.thatAcceptsNone(alphabet)
               : FSAs.trimDanglingStates(builder.buildWith(alphabet));
    }

    @Override
    public <S> LanguageSubsetChecker.Result<S> checkSubsetImpl(FSA<S> subsumer, FSA<S> includer)
    {
        return LanguageSubsetCheckerSingleton.INSTANCE.test(subsumer, includer);
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

        private void handleEpsilonTransitions(Builder<R> carrier, Twin<State> statePair, R epsilonP)
        {
            final State deptP = getState(statePair);
            final State dept1 = statePair.getOne();
            final State dept2 = statePair.getTwo();
            delta1.directSuccessorsOf(dept1, epsilon1).forEach(dest1 -> {
                carrier.addTransition(deptP, getState(dest1, dept2), epsilonP);
            });
            delta2.directSuccessorsOf(dept2, epsilon2).forEach(dest2 -> {
                carrier.addTransition(deptP, getState(dept1, dest2), epsilonP);
            });
        }

        private MutableBiMap<Twin<State>, State> run(Builder<R> carrier, BiFunction<S, T, R> transitionDecider)
        {
            final R epsilonP = transitionDecider.apply(epsilon1, epsilon2);

            // FIXME: currently only handles the single start state FSAs
            final Twin<State> startStatePair = Tuples.twin(fsa1.startState(), fsa2.startState());
            stateMapping.put(startStatePair, States.generate());
            pendingProductStates.add(startStatePair);
            Twin<State> currStatePair;
            while ((currStatePair = pendingProductStates.poll()) != null) {
                final State deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(carrier, currStatePair, epsilonP);
                final State dept1 = currStatePair.getOne();
                final State dept2 = currStatePair.getTwo();
                for (S symbol1 : delta1.nonEpsilonArcLabelsFrom(dept1)) {
                    for (T symbol2 : delta2.nonEpsilonArcLabelsFrom(dept2)) {
                        final R symbolP = transitionDecider.apply(symbol1, symbol2);
                        if (symbolP == null) {
                            continue;
                        }
                        delta1.directSuccessorsOf(dept1, symbol1).forEach(dest1 -> {
                            delta2.directSuccessorsOf(dept2, symbol2).forEach(dest2 -> {
                                carrier.addTransition(deptP, getState(dest1, dest2), symbolP);
                            });
                        });
                    }
                }
            }

            return stateMapping;
        }
    }

    private static final class LanguageSubsetCheckerSingleton
    {
        private static final LanguageSubsetChecker INSTANCE = new BasicLanguageSubsetChecker();
    }
}
