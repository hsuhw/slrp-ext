package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;
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

public interface FSA<S> extends Automaton<S>
{
    @Override
    FSA<S> trimUnreachableStates();

    @Override
    <R> FSA<R> project(Alphabet<R> alphabet, Function<S, R> projector);

    default boolean isDeterministic()
    {
        final Predicate<State<S>> noEpsilonTransAndOnlyOneSucc = state -> {
            final boolean noEpsilonTrans = !state.transitionExists(alphabet().epsilon());
            final boolean onlyOneSucc = state.enabledSymbols()
                                             .allSatisfy(symbol -> state.successors(symbol).size() == 1);
            return noEpsilonTrans && onlyOneSucc;
        };

        return states().allSatisfy(noEpsilonTransAndOnlyOneSucc);
    }

    @Override
    MutableFSA<S> toMutable();

    @Override
    default ImmutableFSA<S> toImmutable()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    default SetIterable<State<S>> incompleteStates()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final SetIterable<S> complete = alphabet().noEpsilonSet();

        return states().reject(which -> which.enabledSymbols().containsAllIterable(complete));
    }

    default boolean isComplete()
    {
        return incompleteStates().size() == 0;
    }

    private boolean acceptsDeterminedly(ListIterable<S> word)
    {
        final S epsilon = alphabet().epsilon();

        State<S> currState = startState();
        SetIterable<State<S>> nextState;
        S symbol;
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

    private boolean acceptsNondeterminedly(ListIterable<S> word)
    {
        final S epsilon = alphabet().epsilon();
        final TransitionGraph<S> delta = transitionGraph();

        SetIterable<State<S>> currStates = Sets.immutable.of(startState()), nextStates;
        S symbol;
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

    default boolean accepts(ListIterable<S> word)
    {
        return alphabet().asSet().containsAllIterable(word) && // valid word given
            (isDeterministic() ? acceptsDeterminedly(word) : acceptsNondeterminedly(word));
    }

    default boolean acceptsNone()
    {
        return !liveStates().anySatisfy(this::isAcceptState);
    }

    private ListIterable<S> getOneShortestWordDeterminedly()
    {
        final int stateNumber = states().size();
        final MutableMap<State<S>, Pair<State<S>, S>> visitRecord = UnifiedMap.newMap(stateNumber); // upper bound
        final Queue<State<S>> pendingChecks = new LinkedList<>();

        final State<S> startState = startState();
        pendingChecks.add(startState);
        State<S> currState;
        while ((currState = pendingChecks.poll()) != null) {
            if (isAcceptState(currState)) {
                final MutableList<S> word = FastList.newList(stateNumber); // upper bound
                while (currState != startState) {
                    final Pair<State<S>, S> visitorAndSymbol = visitRecord.get(currState);
                    word.add(visitorAndSymbol.getTwo());
                    currState = visitorAndSymbol.getOne();
                }
                return word.reverseThis();
            }
            final State<S> visitor = currState; // effectively finalized for the lambda expression
            for (S symbol : currState.enabledSymbols()) {
                visitRecord.computeIfAbsent(visitor.successor(symbol), visited -> {
                    pendingChecks.add(visited);
                    return Tuples.pair(visitor, symbol);
                });
            }
        }

        return null;
    }

    private ListIterable<S> getOneShortestWordNondeterminedly()
    {
        final SetIterable<S> noEpsilonAlphabet = alphabet().noEpsilonSet();
        final TransitionGraph<S> delta = transitionGraph();
        final int stateNumber = states().size(); // upper bound
        final MutableMap<SetIterable<State<S>>, Pair<SetIterable<State<S>>, S>> visitRecord = UnifiedMap
            .newMap(stateNumber);
        final Queue<SetIterable<State<S>>> pendingChecks = new LinkedList<>();

        final SetIterable<State<S>> startStates = transitionGraph().epsilonClosureOf(startState());
        pendingChecks.add(startStates);
        SetIterable<State<S>> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            if (currStates.anySatisfy(this::isAcceptState)) {
                final MutableList<S> word = FastList.newList(stateNumber);
                while (currStates != startStates) {
                    final Pair<SetIterable<State<S>>, S> visitorAndSymbol = visitRecord.get(currStates);
                    word.add(visitorAndSymbol.getTwo());
                    currStates = visitorAndSymbol.getOne();
                }
                return word.reverseThis();
            }
            final SetIterable<State<S>> visitor = currStates; // effectively finalized for the lambda expression
            for (S symbol : noEpsilonAlphabet) {
                visitRecord.computeIfAbsent(delta.epsilonClosureOf(visitor, symbol), touchedStates -> {
                    pendingChecks.add(touchedStates);
                    return Tuples.pair(visitor, symbol);
                });
            }
        }

        return null;
    }

    default ListIterable<S> enumerateOneShortest()
    {
        return isDeterministic() ? getOneShortestWordDeterminedly() : getOneShortestWordNondeterminedly();
    }

    static <S> Twin<ListIterable<State<S>>> splitPartition(ListIterable<State<S>> toSplit,
        RichIterable<State<S>> checkSet, S symbol)
    {
        final ListIterable<State<S>> inSet = toSplit
            .select(eachState -> checkSet.contains(eachState.successor(symbol)));
        final ListIterable<State<S>> outSet = toSplit.reject(inSet::contains);

        return inSet.size() < outSet.size() ? Tuples.twin(inSet, outSet) : Tuples.twin(outSet, inSet);
    }

    default RichIterable<ListIterable<State<S>>> refinePartition(RichIterable<ListIterable<State<S>>> initialPartition,
        ListIterable<State<S>> initialCheckSet)
    {
        final MutableSortedSet<Pair<ListIterable<State<S>>, S>> pendingChecks = TreeSortedSet
            .newSet(Comparator.comparingInt(Object::hashCode));
        final SetIterable<S> symbols = alphabet().noEpsilonSet();
        symbols.forEach(symbol -> pendingChecks.add(Tuples.pair(initialCheckSet, symbol)));

        RichIterable<ListIterable<State<S>>> currPartition = initialPartition;
        while (pendingChecks.notEmpty()) {
            final Pair<ListIterable<State<S>>, S> currCheck = pendingChecks.getFirst();
            pendingChecks.remove(currCheck);
            currPartition = currPartition.flatCollect(part -> {
                final Twin<ListIterable<State<S>>> splitPart = splitPartition(part, currCheck.getOne(),
                                                                              currCheck.getTwo());
                if (splitPart.getOne().notEmpty()) {
                    symbols.forEach(symbol -> {
                        final Pair<ListIterable<State<S>>, S> splitCheck = Tuples.pair(part, symbol);
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

    FSA<S> determinize();

    FSA<S> complete();

    FSA<S> minimize();

    FSA<S> complement();

    FSA<S> intersect(FSA<S> target);

    FSA<S> union(FSA<S> target);

    LanguageSubsetChecker.Result<S> checkContaining(FSA<S> target);

    @Override
    String toString();
}
