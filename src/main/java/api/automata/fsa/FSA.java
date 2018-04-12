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
    FSA<S> trimEpsilonTransitions();

    @Override
    <R> FSA<R> project(Alphabet<R> alphabet, Function<S, R> projector);

    default boolean isDeterministic()
    {
        final Predicate<State<S>> noEpsilonTransAndOnlyOneSucc = state -> {
            final var noEpsilonTrans = !state.transitionExists(alphabet().epsilon());
            final var onlyOneSucc = state.enabledSymbols().allSatisfy(symbol -> state.successors(symbol).size() == 1);
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

        final var complete = alphabet().noEpsilonSet();

        return states().reject(which -> which.enabledSymbols().containsAllIterable(complete));
    }

    default boolean isComplete()
    {
        return incompleteStates().size() == 0;
    }

    private boolean acceptsDeterminedly(ListIterable<S> word)
    {
        final var epsilon = alphabet().epsilon();

        var currState = startState();
        SetIterable<State<S>> nextState;
        S symbol;
        for (var readHead = 0; readHead < word.size(); readHead++) {
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
        final var epsilon = alphabet().epsilon();
        final var delta = transitionGraph();

        SetIterable<State<S>> currStates = Sets.immutable.of(startState()), nextStates;
        S symbol;
        for (var readHead = 0; readHead < word.size(); readHead++) {
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
        return !reachableStates().anySatisfy(this::isAcceptState);
    }

    private ListIterable<S> getOneShortestWordDeterminedly()
    {
        final var stateNumber = states().size();
        final MutableMap<State<S>, Pair<State<S>, S>> visitRecord = UnifiedMap.newMap(stateNumber); // upper bound
        final Queue<State<S>> pendingChecks = new LinkedList<>();

        final var startState = startState();
        pendingChecks.add(startState);
        State<S> currState;
        while ((currState = pendingChecks.poll()) != null) {
            if (isAcceptState(currState)) {
                final MutableList<S> word = FastList.newList(stateNumber); // upper bound
                while (currState != startState) {
                    final var visitorAndSymbol = visitRecord.get(currState);
                    word.add(visitorAndSymbol.getTwo());
                    currState = visitorAndSymbol.getOne();
                }
                return word.reverseThis();
            }
            final var visitor = currState; // effectively finalized for the lambda expression
            visitor.transitions()
                   .forEach(symbolAndDest -> visitRecord.computeIfAbsent(symbolAndDest.getTwo(), visited -> {
                       pendingChecks.add(visited);
                       return Tuples.pair(visitor, symbolAndDest.getOne());
                   }));
        }

        return null;
    }

    private ListIterable<S> getOneShortestWordNondeterminedly()
    {
        final var noEpsilonAlphabet = alphabet().noEpsilonSet();
        final var delta = transitionGraph();
        final var stateNumber = states().size(); // upper bound
        final MutableMap<SetIterable<State<S>>, Pair<SetIterable<State<S>>, S>> visitRecord = UnifiedMap
            .newMap(stateNumber);
        final Queue<SetIterable<State<S>>> pendingChecks = new LinkedList<>();

        final var startStates = transitionGraph().epsilonClosureOf(startState());
        pendingChecks.add(startStates);
        SetIterable<State<S>> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            if (currStates.anySatisfy(this::isAcceptState)) {
                final MutableList<S> word = FastList.newList(stateNumber);
                while (currStates != startStates) {
                    final var visitorAndSymbol = visitRecord.get(currStates);
                    word.add(visitorAndSymbol.getTwo());
                    currStates = visitorAndSymbol.getOne();
                }
                return word.reverseThis();
            }
            final var visitor = currStates; // effectively finalized for the lambda expression
            for (var symbol : noEpsilonAlphabet) {
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
        final var inSet = toSplit.select(eachState -> checkSet.contains(eachState.successor(symbol)));
        final var outSet = toSplit.reject(inSet::contains);

        return inSet.size() < outSet.size() ? Tuples.twin(inSet, outSet) : Tuples.twin(outSet, inSet);
    }

    default RichIterable<ListIterable<State<S>>> refinePartition(RichIterable<ListIterable<State<S>>> initialPartition,
        ListIterable<State<S>> initialCheckSet)
    {
        final MutableSortedSet<Pair<ListIterable<State<S>>, S>> pendingChecks = TreeSortedSet
            .newSet(Comparator.comparingInt(Object::hashCode));
        final var symbols = alphabet().noEpsilonSet();
        symbols.forEach(symbol -> pendingChecks.add(Tuples.pair(initialCheckSet, symbol)));

        var currPartition = initialPartition;
        while (pendingChecks.notEmpty()) {
            final var currCheck = pendingChecks.getFirst();
            pendingChecks.remove(currCheck);
            currPartition = currPartition.flatCollect(part -> {
                final var splitPart = splitPartition(part, currCheck.getOne(), currCheck.getTwo());
                if (splitPart.getOne().notEmpty()) {
                    symbols.forEach(symbol -> {
                        final var splitCheck = Tuples.pair(part, symbol);
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

    LanguageSubsetChecker.Result<S> checkContainingWithCounterSource(FSA<S> target);

    @Override
    String toString();
}
