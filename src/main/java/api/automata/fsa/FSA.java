package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface FSA<S extends State<T>, T> extends Automaton<S, T>
{
    @Override
    FSA<? extends State<T>, T> trimUnreachableStates();

    @Override
    <R> FSA<? extends State<R>, R> project(Alphabet<R> alphabet, Function<T, R> projector);

    @Override
    <U extends State<V>, V, R> FSA<? extends MutableState<R>, R> product(Automaton<U, V> target, Alphabet<R> alphabet,
        StepMaker<S, T, U, V, R> stepMaker, Finalizer<S, U, MutableState<R>, R> finalizer);

    default boolean isDeterministic()
    {
        final Predicate<? super S> noEpsilonTransAndOnlyOneSucc = state -> {
            final boolean noEpsilonTrans = !state.transitionExists(alphabet().epsilon());
            final boolean onlyOneSucc = state.enabledSymbols()
                                             .allSatisfy(symbol -> state.successors(symbol).size() == 1);
            return noEpsilonTrans && onlyOneSucc;
        };

        return states().allSatisfy(noEpsilonTransAndOnlyOneSucc);
    }

    @Override
    MutableFSA<? extends MutableState<T>, T> toMutable();

    @Override
    default ImmutableFSA<? extends ImmutableState<T>, T> toImmutable()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    default SetIterable<S> incompleteStates()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final SetIterable<T> complete = alphabet().noEpsilonSet();

        return states().reject(which -> which.enabledSymbols().containsAllIterable(complete));
    }

    default boolean isComplete()
    {
        return incompleteStates().size() == 0;
    }

    private boolean acceptsDeterminedly(ListIterable<T> word)
    {
        final T epsilon = alphabet().epsilon();

        State<T> currState = startState();
        SetIterable<? extends State<T>> nextState;
        T symbol;
        for (int readHead = 0; readHead < word.size(); readHead++) {
            symbol = word.get(readHead);
            if (symbol.equals(epsilon)) {
                continue;
            }
            if ((nextState = currState.successors(symbol)).isEmpty()) {
                return false;
            }
            currState = nextState.getOnly();
        }

        return isAcceptState(currState);
    }

    private boolean acceptsNondeterminedly(ListIterable<T> word)
    {
        final T epsilon = alphabet().epsilon();
        final api.automata.Automaton.TransitionGraph<S, T> delta = transitionGraph();

        SetIterable<S> currStates = Sets.immutable.of(startState()), nextStates;
        T symbol;
        for (int readHead = 0; readHead < word.size(); readHead++) {
            symbol = word.get(readHead);
            if (symbol.equals(epsilon)) {
                continue;
            }
            if ((nextStates = delta.epsilonClosureOf(currStates, symbol)).isEmpty()) {
                return false;
            }
            currStates = nextStates;
        }

        return currStates.anySatisfy(this::isAcceptState);
    }

    default boolean accepts(ListIterable<T> word)
    {
        return alphabet().asSet().containsAllIterable(word) && // valid word given
            (isDeterministic() ? acceptsDeterminedly(word) : acceptsNondeterminedly(word));
    }

    default boolean acceptsNone()
    {
        return !liveStates().anySatisfy(this::isAcceptState);
    }

    private ListIterable<T> getOneShortestWordDeterminedly()
    {
        final int stateNumber = states().size();
        final MutableMap<State<T>, Pair<State<T>, T>> visitRecord = UnifiedMap.newMap(stateNumber); // upper bound
        final Queue<State<T>> pendingChecks = new LinkedList<>();

        final S startState = startState();
        pendingChecks.add(startState);
        State<T> currState;
        while ((currState = pendingChecks.poll()) != null) {
            if (isAcceptState(currState)) {
                final MutableList<T> word = FastList.newList(stateNumber); // upper bound
                while (currState != startState) {
                    final Pair<State<T>, T> visitorAndSymbol = visitRecord.get(currState);
                    word.add(visitorAndSymbol.getTwo());
                    currState = visitorAndSymbol.getOne();
                }
                return word.reverseThis();
            }
            final State<T> visitor = currState; // effectively finalized for the lambda expression
            for (T symbol : currState.enabledSymbols()) {
                visitRecord.computeIfAbsent(visitor.successor(symbol), visited -> {
                    pendingChecks.add(visited);
                    return Tuples.pair(visitor, symbol);
                });
            }
        }

        return null;
    }

    private ListIterable<T> getOneShortestWordNondeterminedly()
    {
        final SetIterable<T> noEpsilonAlphabet = alphabet().noEpsilonSet();
        final api.automata.Automaton.TransitionGraph<S, T> delta = transitionGraph();
        final int stateNumber = states().size(); // upper bound
        final MutableMap<SetIterable<S>, Pair<SetIterable<S>, T>> visitRecord = UnifiedMap.newMap(stateNumber);
        final Queue<SetIterable<S>> pendingChecks = new LinkedList<>();

        final SetIterable<S> startStates = transitionGraph().epsilonClosureOf(startState());
        pendingChecks.add(startStates);
        SetIterable<S> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            if (currStates.anySatisfy(this::isAcceptState)) {
                final MutableList<T> word = FastList.newList(stateNumber);
                while (currStates != startStates) {
                    final Pair<SetIterable<S>, T> visitorAndSymbol = visitRecord.get(currStates);
                    word.add(visitorAndSymbol.getTwo());
                    currStates = visitorAndSymbol.getOne();
                }
                return word.reverseThis();
            }
            final SetIterable<S> visitor = currStates; // effectively finalized for the lambda expression
            for (T symbol : noEpsilonAlphabet) {
                visitRecord.computeIfAbsent(delta.epsilonClosureOf(visitor, symbol), touchedStates -> {
                    pendingChecks.add(touchedStates);
                    return Tuples.pair(visitor, symbol);
                });
            }
        }

        return null;
    }

    default ListIterable<T> enumerateOneShortest()
    {
        return isDeterministic() ? getOneShortestWordDeterminedly() : getOneShortestWordNondeterminedly();
    }

    static <S extends State<T>, T> Twin<ListIterable<S>> splitPartition(ListIterable<S> toBeSplit,
        RichIterable<S> checkSet, T symbol)
    {
        final ListIterable<S> inSet = toBeSplit.select(eachState -> checkSet.contains(eachState.successor(symbol)));
        final ListIterable<S> outSet = toBeSplit.reject(inSet::contains);

        return inSet.size() < outSet.size() ? Tuples.twin(inSet, outSet) : Tuples.twin(outSet, inSet);
    }

    default RichIterable<ListIterable<S>> refinePartition(RichIterable<ListIterable<S>> initialPartition,
        ListIterable<S> initialCheckSet)
    {
        final MutableSortedSet<Pair<ListIterable<S>, T>> pendingChecks = TreeSortedSet
            .newSet(Comparator.comparingInt(Object::hashCode));
        final SetIterable<T> symbols = alphabet().noEpsilonSet();
        symbols.forEach(symbol -> pendingChecks.add(Tuples.pair(initialCheckSet, symbol)));

        RichIterable<ListIterable<S>> currPartition = initialPartition;
        while (pendingChecks.notEmpty()) {
            final Pair<ListIterable<S>, T> currCheck = pendingChecks.getFirst();
            pendingChecks.remove(currCheck);
            currPartition = currPartition.flatCollect(part -> {
                final Twin<ListIterable<S>> splitPart = splitPartition(part, currCheck.getOne(), currCheck.getTwo());
                if (splitPart.getOne().notEmpty()) {
                    symbols.forEach(symbol -> {
                        final Pair<ListIterable<S>, T> splitCheck = Tuples.pair(part, symbol);
                        if (pendingChecks.contains(splitCheck)) {
                            pendingChecks.remove(splitCheck);
                            pendingChecks.add(Tuples.pair(splitPart.getOne(), symbol));
                            pendingChecks.add(Tuples.pair(splitPart.getTwo(), symbol));
                        } else {
                            pendingChecks.add(Tuples.pair(splitPart.getOne(), symbol));
                        }
                    });
                    return Lists.immutable.of(splitPart.getOne(), splitPart.getTwo());
                }
                return Lists.immutable.of(part);
            });
        }

        return currPartition;
    }

    FSA<? extends State<T>, T> determinize();

    FSA<? extends State<T>, T> complete();

    FSA<? extends State<T>, T> minimize();

    FSA<? extends State<T>, T> complement();

    FSA<? extends State<T>, T> intersect(FSA<? extends State<T>, T> target);

    FSA<? extends State<T>, T> union(FSA<? extends State<T>, T> target);

    LanguageSubsetChecker.Result<T> checkContaining(FSA<? extends State<T>, T> target);

    /**
     * This method only make sense when there is no possible contravariance
     * introduced in this interface.
     */
    static <S extends State<T>, T> FSA<State<T>, T> upcast(FSA<S, T> derivative)
    {
        @SuppressWarnings("unchecked")
        final FSA<State<T>, T> generalized = (FSA<State<T>, T>) derivative;
        return generalized;
    }

    @Override
    String toString();
}
