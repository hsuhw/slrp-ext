package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.DeltaFunction;
import api.automata.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

import static api.util.Values.NOT_IMPLEMENTED_YET;

public interface FSA<S> extends Automaton<S>
{
    default SetIterable<State> getIncompleteStates()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final DeltaFunction<S> delta = getDeltaFunction();
        final ImmutableSet<S> completeAlphabet = getAlphabet().noEpsilonSet();

        return getStates().select(state -> {
            return !delta.enabledSymbolsOn(state).containsAllIterable(completeAlphabet);
        });
    }

    default boolean isComplete()
    {
        return getIncompleteStates().size() == 0;
    }

    default ImmutableList<S> enumerateOneShortestWord()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        final DeltaFunction<S> delta = getDeltaFunction();
        final MutableMap<State, Pair<State, S>> touchedBy = UnifiedMap.newMap(getStateNumber()); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();
        final State startState = getStartState();
        pendingChecks.add(startState);
        State currState;
        while ((currState = pendingChecks.poll()) != null) {
            if (isAcceptState(currState)) {
                final MutableList<S> word = FastList.newList(getStateNumber()); // upper bound
                while (currState != startState) {
                    final Pair<State, S> touch = touchedBy.get(currState);
                    word.add(touch.getTwo());
                    currState = touch.getOne();
                }
                return word.reverseThis().toImmutable();
            }
            final State state = currState; // effectively finalize for lambda expressions
            for (S symbol : delta.enabledSymbolsOn(currState)) {
                touchedBy.computeIfAbsent(delta.successorOf(state, symbol), touchedState -> {
                    pendingChecks.add(touchedState);
                    return Tuples.pair(state, symbol);
                });
            }
        }

        return null;
    }

    interface Builder<S> extends Automaton.Builder<S>
    {
        Alphabet<S> getCurrentAlphabet();

        @Override
        Builder<S> addSymbol(S symbol);

        @Override
        Builder<S> addState(State state);

        @Override
        Builder<S> removeState(State state);

        @Override
        Builder<S> addStartState(State state);

        @Override
        Builder<S> addStartStates(SetIterable<State> states);

        @Override
        Builder<S> resetStartStates();

        @Override
        Builder<S> addAcceptState(State state);

        @Override
        Builder<S> addAcceptStates(SetIterable<State> states);

        @Override
        Builder<S> resetAcceptStates();

        @Override
        Builder<S> addTransition(State dept, State dest, S symbol);

        @Override
        FSA<S> build();

        FSA<S> build(Alphabet<S> alphabet);
    }
}
