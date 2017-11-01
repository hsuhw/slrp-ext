package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

public interface FSA<S> extends Automaton<S>
{
    default boolean isDeterministic()
    {
        return transitionGraph().arcDeterministic() && startStates().size() == 1;
    }

    default ImmutableSet<State> incompleteStates()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final TransitionGraph<State, S> delta = transitionGraph();
        final ImmutableSet<S> complete = alphabet().noEpsilonSet();

        return states().select(state -> !delta.enabledArcsOn(state).containsAllIterable(complete));
    }

    default boolean isComplete()
    {
        return incompleteStates().size() == 0;
    }

    private boolean deterministicallyAccepts(ImmutableList<S> word)
    {
        final TransitionGraph<State, S> delta = transitionGraph();
        final S epsilon = delta.epsilonLabel();

        State currState = startState();
        ImmutableSet<State> nextState;
        S symbol;
        for (int readHead = 0; readHead < word.size(); readHead++) {
            symbol = word.get(readHead);
            if (symbol.equals(epsilon)) {
                continue;
            }
            if ((nextState = delta.successorsOf(currState, symbol)).isEmpty()) {
                return false;
            }
            currState = nextState.getOnly();
        }

        return isAcceptState(currState);
    }

    private boolean nondeterministicallyAccepts(ImmutableList<S> word)
    {
        final TransitionGraph<State, S> delta = transitionGraph();
        final S epsilon = delta.epsilonLabel();

        SetIterable<State> currStates = startStates(), nextStates;
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

    default boolean accepts(ImmutableList<S> word)
    {
        return alphabet().set().containsAllIterable(word) && // valid word given
            (isDeterministic() ? deterministicallyAccepts(word) : nondeterministicallyAccepts(word));
    }

    private ImmutableList<S> deterministicallyEnumerateOneShortestWord()
    {
        final TransitionGraph<State, S> delta = transitionGraph();
        final int stateNumber = states().size();
        final MutableMap<State, Pair<State, S>> touchedBy = UnifiedMap.newMap(stateNumber); // upper bound
        final Queue<State> pendingChecks = new LinkedList<>();

        final State startState = startState();
        pendingChecks.add(startState);
        State currState;
        while ((currState = pendingChecks.poll()) != null) {
            if (isAcceptState(currState)) {
                final MutableList<S> word = FastList.newList(stateNumber); // upper bound
                while (currState != startState) {
                    final Pair<State, S> touch = touchedBy.get(currState);
                    word.add(touch.getTwo());
                    currState = touch.getOne();
                }
                return word.reverseThis().toImmutable();
            }
            final State state = currState; // effectively finalized for the lambda expression
            for (S symbol : delta.enabledArcsOn(currState)) {
                touchedBy.computeIfAbsent(delta.successorOf(state, symbol), touchedState -> {
                    pendingChecks.add(touchedState);
                    return Tuples.pair(state, symbol);
                });
            }
        }

        return null;
    }

    private ImmutableList<S> nondeterministicallyEnumerateOneShortestWord()
    {
        final ImmutableSet<S> noEpsilonAlphabet = alphabet().noEpsilonSet();
        final TransitionGraph<State, S> delta = transitionGraph();
        final int stateNumber = states().size(); // upper bound
        final MutableMap<SetIterable<State>, Pair<SetIterable<State>, S>> touchedBy = UnifiedMap.newMap(stateNumber);
        final Queue<SetIterable<State>> pendingChecks = new LinkedList<>();

        final SetIterable<State> startStates = startStates();
        pendingChecks.add(startStates);
        SetIterable<State> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            if (currStates.anySatisfy(this::isAcceptState)) {
                final MutableList<S> word = FastList.newList(stateNumber);
                while (currStates != startStates) {
                    final Pair<SetIterable<State>, S> touch = touchedBy.get(currStates);
                    word.add(touch.getTwo());
                    currStates = touch.getOne();
                }
                return word.reverseThis().toImmutable();
            }
            final SetIterable<State> states = currStates; // effectively finalized for the lambda expression
            for (S symbol : noEpsilonAlphabet) {
                touchedBy.computeIfAbsent(delta.epsilonClosureOf(states, symbol), touchedStates -> {
                    pendingChecks.add(touchedStates);
                    return Tuples.pair(states, symbol);
                });
            }
        }

        return null;
    }

    default ImmutableList<S> enumerateOneShortestWord()
    {
        return isDeterministic()
               ? deterministicallyEnumerateOneShortestWord()
               : nondeterministicallyEnumerateOneShortestWord();
    }

    @Override
    String toString();

    interface Builder<S> extends Automaton.Builder<S>
    {
        Alphabet<S> currentAlphabet();

        int currentStateNumber();

        int currentStartStateNumber();

        int currentAcceptStateNumber();

        int currentTransitionNumber();

        @Override
        Builder<S> addSymbol(S symbol);

        @Override
        Builder<S> addState(State state);

        @Override
        Builder<S> removeState(State state);

        @Override
        Builder<S> addStartState(State state);

        @Override
        Builder<S> addStartStates(ImmutableSet<State> states);

        @Override
        Builder<S> resetStartStates();

        @Override
        Builder<S> addAcceptState(State state);

        @Override
        Builder<S> addAcceptStates(ImmutableSet<State> states);

        @Override
        Builder<S> resetAcceptStates();

        @Override
        Builder<S> addTransition(State dept, State dest, S symbol);

        @Override
        FSA<S> build();

        FSA<S> build(Alphabet<S> alphabet);
    }

    interface Provider
    {
        <S> Builder<S> builder(int stateCapacity, int symbolCapacity, S epsilonSymbol);

        <S> Builder<S> builderOn(FSA<S> fsa);

        default <S> FSA<S> thatAcceptsNone(Alphabet<S> alphabet)
        {
            final Builder<S> builder = builder(1, alphabet.size(), alphabet.epsilon());

            final State state = States.generate();
            builder.addStartState(state);
            alphabet.noEpsilonSet().forEach(symbol -> builder.addTransition(state, state, symbol));

            return builder.build(alphabet);
        }

        default <S> FSA<S> thatAcceptsAll(Alphabet<S> alphabet)
        {
            final Builder<S> builder = builder(1, alphabet.size(), alphabet.epsilon());

            final State state = States.generate();
            builder.addStartState(state).addAcceptState(state);
            alphabet.noEpsilonSet().forEach(symbol -> builder.addTransition(state, state, symbol));

            return builder.build(alphabet);
        }

        default <S> FSA<S> thatAcceptsOnly(Alphabet<S> alphabet, ImmutableList<S> word)
        {
            return thatAcceptsOnly(alphabet, Lists.immutable.of(word));
        }

        default <S> FSA<S> thatAcceptsOnly(Alphabet<S> alphabet, RichIterable<ImmutableList<S>> words)
        {
            final int stateCapacity = (int) words.collectInt(ImmutableList::size).sum(); // upper bound
            final Builder<S> builder = builder(stateCapacity, alphabet.size(), alphabet.epsilon());

            final State startState = States.generate();
            final State acceptState = States.generate();
            builder.addAcceptState(acceptState);
            State currState = startState, nextState;
            int lastSymbolPos;
            S symbol;
            builder.addStartState(startState);
            for (ImmutableList<S> word : words) {
                for (int i = 0; i < (lastSymbolPos = word.size() - 1); i++) {
                    if (!(symbol = word.get(i)).equals(alphabet.epsilon())) {
                        nextState = States.generate();
                        builder.addTransition(currState, nextState, symbol);
                        currState = nextState;
                    }
                }
                builder.addTransition(currState, acceptState, word.get(lastSymbolPos));
                currState = startState;
            }

            return builder.build(alphabet);
        }

        FSAManipulator manipulator();
    }
}
