package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.list.ImmutableList;
import util.Misc;

public interface FSA<S extends Symbol> extends Automaton<S>
{
    default boolean isDeterministic()
    {
        return this instanceof Deterministic;
    }

    default boolean isComplete()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("N/A on nondeterministic FSA");
        }
        final int completeSize = getAlphabet().size() - 1; // without epsilon
        for (State s : getStates()) {
            if (getTransitionFunction().enabledSymbolsOn(s).size() != completeSize) {
                return false;
            }
        }
        return true;
    }

    Alphabet<S> getAlphabet();

    default int getAlphabetSize()
    {
        return getAlphabet().size();
    }

    default State getStartState()
    {
        for (State s : getStates()) {
            if (isStartState(s)) {
                return s; // the first start state
            }
        }
        throw new UnsupportedOperationException("automaton with no start states");
    }

    default boolean accepts(ImmutableList<S> word)
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException(Misc.NIY);
        }
        State currState = getStartState(), nextState;
        final TransitionFunction<S> delta = getTransitionFunction();
        for (int readHead = 0; readHead < word.size(); readHead++) {
            nextState = delta.successorOf(currState, word.get(readHead));
            if (nextState == null) {
                return false;
            }
            currState = nextState;
        }
        return isAcceptState(currState);
    }

    interface Builder<S extends Symbol> extends Automaton.Builder<S>
    {
        Alphabet<S> getCurrentAlphabet();

        void addSymbol(S symbol);

        @Override
        FSA<S> build();

        FSA<S> build(Alphabet<S> alphabetOverride);
    }
}
