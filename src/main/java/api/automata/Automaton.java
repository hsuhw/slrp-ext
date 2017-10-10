package api.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

import static api.util.Values.NOT_IMPLEMENTED_YET;

public interface Automaton<S>
{
    default boolean isDeterministic()
    {
        return this instanceof Deterministic;
    }

    Alphabet<S> getAlphabet();

    default int getAlphabetSize()
    {
        return getAlphabet().size();
    }

    ImmutableSet<State> getStates();

    default int getStateNumber()
    {
        return getStates().size();
    }

    SetIterable<State> getStartStates();

    default State getStartState()
    {
        if (getStartStates().size() != 1) {
            throw new UnsupportedOperationException("more than one start states");
        }

        return getStartStates().getOnly();
    }

    default boolean isStartState(State state)
    {
        return getStartStates().contains(state);
    }

    SetIterable<State> getAcceptStates();

    default boolean isAcceptState(State state)
    {
        return getAcceptStates().contains(state);
    }

    default SetIterable<State> getNonAcceptStates()
    {
        return getStates().newWithoutAll(getAcceptStates());
    }

    DeltaFunction<S> getDeltaFunction();

    default boolean accepts(ImmutableList<S> word)
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }
        if (!getAlphabet().getSet().containsAllIterable(word)) {
            return false;
        }

        State currState = getStartState(), nextState;
        S symbol;
        final DeltaFunction<S> delta = getDeltaFunction();
        for (int readHead = 0; readHead < word.size(); readHead++) {
            symbol = word.get(readHead);
            if (symbol.equals(getAlphabet().getEpsilonSymbol())) {
                continue;
            }
            nextState = delta.successorOf(currState, symbol);
            if (nextState == null) {
                return false;
            }
            currState = nextState;
        }

        return isAcceptState(currState);
    }

    interface Builder<S>
    {
        Builder<S> addSymbol(S symbol);

        Builder<S> addState(State state);

        Builder<S> removeState(State state);

        Builder<S> addStartState(State state);

        Builder<S> addStartStates(SetIterable<State> states);

        Builder<S> resetStartStates();

        Builder<S> addAcceptState(State state);

        Builder<S> addAcceptStates(SetIterable<State> states);

        Builder<S> resetAcceptStates();

        Builder<S> addTransition(State dept, State dest, S symbol);

        Automaton<S> build();
    }
}
