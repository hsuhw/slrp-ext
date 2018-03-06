package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.MutableState;
import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fsa.MutableFSA;
import core.automata.MapSetState;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

public class BasicMutableFSA<S> extends AbstractMutableFSA<S> implements MutableFSA<S>
{
    private LanguageSubsetChecker<S> languageSubsetChecker;

    public BasicMutableFSA(Alphabet<S> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);

        languageSubsetChecker = new LightLanguageSubsetChecker<>();
    }

    public BasicMutableFSA(BasicMutableFSA<S> toCopy, boolean deep)
    {
        super(toCopy, deep);

        languageSubsetChecker = toCopy.languageSubsetChecker;
    }

    @Override
    public FSA<S> trimUnreachableStates()
    {
        final RichIterable<State<S>> unreachableStates = unreachableStates();
        if (unreachableStates.isEmpty()) {
            return this;
        }

        final BasicMutableFSA<S> result = new BasicMutableFSA<>(this, false);
        result.states.removeAllIterable(unreachableStates);

        return result;
    }

    @Override
    public <T, R> Automaton<R> product(Automaton<T> target, Alphabet<R> alphabet, StepMaker<S, T, R> stepMaker,
        Finalizer<S, T, R> finalizer)
    {
        return new ProductHandler<>(alphabet, target).makeProduct(stepMaker).settle(finalizer);
    }

    @Override
    public LanguageSubsetChecker.Result<S> checkContaining(FSA<S> target)
    {
        return languageSubsetChecker.test(target, this);
    }

    @Override
    protected MutableState<S> createState()
    {
        return new MapSetState<>(alphabet.size());
    }

    @Override
    public String toString()
    {
        return toString("", "");
    }

    private class ProductHandler<T, R>
    {
        private final Automaton<T> target;
        private final S epsilon1;
        private final T epsilon2;
        private final BasicMutableFSA<R> result;
        private final MutableBiMap<Pair<State<S>, State<T>>, MutableState<R>> stateMapping;
        private final Queue<Pair<State<S>, State<T>>> pendingChecks;

        private ProductHandler(Alphabet<R> alphabet, Automaton<T> target)
        {
            final int capacity = states().size() * target.states().size(); // upper bound
            this.target = target;
            epsilon1 = alphabet().epsilon();
            epsilon2 = target.alphabet().epsilon();
            result = new BasicMutableFSA<>(alphabet, capacity);
            stateMapping = new HashBiMap<>(capacity);
            pendingChecks = new LinkedList<>();
        }

        private MutableState<R> takeState(Pair<State<S>, State<T>> statePair)
        {
            return stateMapping.computeIfAbsent(statePair, pair -> {
                pendingChecks.add(pair);
                return result.newState();
            });
        }

        private MutableState<R> takeState(State<S> one, State<T> two)
        {
            return takeState(Tuples.pair(one, two));
        }

        private void handleEpsilonTransitions(Pair<State<S>, State<T>> statePair)
        {
            final MutableState<R> deptP = takeState(statePair);
            final State<S> dept1 = statePair.getOne();
            final State<T> dept2 = statePair.getTwo();
            dept1.successors(epsilon1).forEach(dest -> result.addEpsilonTransition(deptP, takeState(dest, dept2)));
            dept2.successors(epsilon2).forEach(dest -> {
                result.addEpsilonTransition(deptP, takeState(dept1, dest));
            });
        }

        private ProductHandler<T, R> makeProduct(StepMaker<S, T, R> stepMaker)
        {
            takeState(startState(), target.startState());
            Pair<State<S>, State<T>> currStatePair;
            while ((currStatePair = pendingChecks.poll()) != null) {
                final MutableState<R> deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(currStatePair);
                final State<S> dept1 = currStatePair.getOne();
                final State<T> dept2 = currStatePair.getTwo();
                // stepMaker.apply(currStatePair, epsilon1, epsilon2); // TODO: check why do we need this
                for (S symbol1 : dept1.enabledSymbols()) {
                    if (symbol1.equals(epsilon1)) {
                        continue; // already handled
                    }
                    for (T symbol2 : dept2.enabledSymbols()) {
                        if (symbol2.equals(epsilon2)) {
                            continue; // already handled
                        }
                        final R symbolP = stepMaker.apply(currStatePair, symbol1, symbol2);
                        if (symbolP == null) {
                            continue; // no step should be made
                        }
                        dept1.successors(symbol1).forEach(dest1 -> dept2.successors(symbol2).forEach(dest2 -> {
                            result.addTransition(deptP, takeState(dest1, dest2), symbolP);
                        }));
                    }
                }
            }

            return this;
        }

        private MutableFSA<R> settle(Finalizer<S, T, R> finalizer)
        {
            finalizer.apply(stateMapping, result);

            return result;
        }
    }
}
