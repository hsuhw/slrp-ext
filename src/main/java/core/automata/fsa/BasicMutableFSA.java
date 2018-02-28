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

public class BasicMutableFSA<T> extends AbstractMutableFSA<MutableState<T>, T> implements MutableFSA<MutableState<T>, T>
{
    private LanguageSubsetChecker<T> languageSubsetChecker;

    public BasicMutableFSA(Alphabet<T> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);

        languageSubsetChecker = new LightLanguageSubsetChecker<>();
    }

    public BasicMutableFSA(BasicMutableFSA<T> toBeCopied, boolean deep)
    {
        super(toBeCopied, deep);

        languageSubsetChecker = toBeCopied.languageSubsetChecker;
    }

    @Override
    public FSA<? extends State<T>, T> trimUnreachableStates()
    {
        final RichIterable<MutableState<T>> toBeTrimmed = unreachableStates();
        if (toBeTrimmed.isEmpty()) {
            return this;
        }

        final BasicMutableFSA<T> result = new BasicMutableFSA<>(this, false);
        result.states.removeAllIterable(toBeTrimmed);

        return result;
    }

    @Override
    public <U extends State<V>, V, R> MutableFSA<? extends MutableState<R>, R> product(Automaton<U, V> target,
        Alphabet<R> alphabet, StepMaker<MutableState<T>, T, U, V, R> stepMaker,
        Finalizer<MutableState<T>, U, MutableState<R>, R> finalizer)
    {
        return new ProductHandler<>(alphabet, target).makeProduct(stepMaker).settle(finalizer);
    }

    @Override
    public LanguageSubsetChecker.Result<T> checkContaining(FSA<? extends State<T>, T> target)
    {
        return languageSubsetChecker.test(FSA.upcast(target), FSA.upcast(this));
    }

    @Override
    protected MutableState<T> createState()
    {
        return new MapSetState<>(alphabet.size());
    }

    @Override
    public String toString()
    {
        return toString("", "");
    }

    private class ProductHandler<U extends State<V>, V, R>
    {
        private final Automaton<U, V> target;
        private final T epsilon1;
        private final V epsilon2;
        private final BasicMutableFSA<R> result;
        private final MutableBiMap<Pair<MutableState<T>, U>, MutableState<R>> stateMapping;
        private final Queue<Pair<MutableState<T>, U>> pendingChecks;

        private ProductHandler(Alphabet<R> alphabet, Automaton<U, V> target)
        {
            final int capacity = states().size() * target.states().size(); // upper bound
            this.target = target;
            epsilon1 = alphabet().epsilon();
            epsilon2 = target.alphabet().epsilon();
            result = new BasicMutableFSA<>(alphabet, capacity);
            stateMapping = new HashBiMap<>(capacity);
            pendingChecks = new LinkedList<>();
        }

        private MutableState<R> takeState(Pair<MutableState<T>, U> statePair)
        {
            return stateMapping.computeIfAbsent(statePair, pair -> {
                pendingChecks.add(pair);
                return result.newState();
            });
        }

        private MutableState<R> takeState(MutableState<T> one, U two)
        {
            return takeState(Tuples.pair(one, two));
        }

        private void handleEpsilonTransitions(Pair<MutableState<T>, U> statePair)
        {
            final MutableState<R> deptP = takeState(statePair);
            final MutableState<T> dept1 = statePair.getOne();
            final U dept2 = statePair.getTwo();
            dept1.successors(epsilon1).forEach(dest -> result.addEpsilonTransition(deptP, takeState(dest, dept2)));
            dept2.successors(epsilon2).forEach(dest -> {
                @SuppressWarnings("unchecked")
                final U destCasted = (U) dest;
                result.addEpsilonTransition(deptP, takeState(dept1, destCasted));
            });
        }

        private ProductHandler<U, V, R> makeProduct(StepMaker<MutableState<T>, T, U, V, R> stepMaker)
        {
            takeState(startState(), target.startState());
            Pair<MutableState<T>, U> currStatePair;
            while ((currStatePair = pendingChecks.poll()) != null) {
                final MutableState<R> deptP = stateMapping.get(currStatePair);
                handleEpsilonTransitions(currStatePair);
                final MutableState<T> dept1 = currStatePair.getOne();
                final U dept2 = currStatePair.getTwo();
                // stepMaker.apply(currStatePair, epsilon1, epsilon2); // TODO: check why do we need this
                for (T symbol1 : dept1.enabledSymbols()) {
                    if (symbol1.equals(epsilon1)) {
                        continue; // already handled
                    }
                    for (V symbol2 : dept2.enabledSymbols()) {
                        if (symbol2.equals(epsilon2)) {
                            continue; // already handled
                        }
                        final R symbolP = stepMaker.apply(currStatePair, symbol1, symbol2);
                        if (symbolP == null) {
                            continue; // no step should be made
                        }
                        dept1.successors(symbol1).forEach(dest1 -> dept2.successors(symbol2).forEach(dest2 -> {
                            @SuppressWarnings("unchecked")
                            final U dest2Casted = (U) dest2;
                            result.addTransition(deptP, takeState(dest1, dest2Casted), symbolP);
                        }));
                    }
                }
            }
            return this;
        }

        private MutableFSA<MutableState<R>, R> settle(Finalizer<MutableState<T>, U, MutableState<R>, R> finalizer)
        {
            finalizer.apply(stateMapping, result);

            return result;
        }
    }
}
