package api.automata;

import api.util.Values;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.SetIterable;

import java.util.List;

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

    SetIterable<State> getStates();

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

    DeltaFunction<S> getDeltaFunction();

    default boolean accepts(ImmutableList<S> word)
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException(Values.NOT_IMPLEMENTED_YET);
        }
        if (!getAlphabet().getSet().containsAll((List) word)) {
            return false;
        }

        State currState = getStartState(), nextState;
        final DeltaFunction<S> delta = getDeltaFunction();
        for (int readHead = 0; readHead < word.size(); readHead++) {
            nextState = delta.successorOf(currState, word.get(readHead));
            if (nextState == null) {
                return false;
            }
            currState = nextState;
        }

        return isAcceptState(currState);
    }

    interface Builder<S>
    {
        Builder<S> addState(State state);

        Builder<S> addStartState(State state);

        Builder<S> addAcceptState(State state);

        Builder<S> addSymbol(S symbol);

        Builder<S> addTransition(State dept, State dest, S symbol);

        Automaton<S> build();
    }
}
