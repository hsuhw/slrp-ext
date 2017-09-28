package api.automata;

import org.eclipse.collections.api.set.SetIterable;

public interface DeltaFunction<S>
{
    int size();

    SetIterable<State> getAllReferredStates();

    SetIterable<S> getAllReferredSymbols();

    S getEpsilonSymbol();

    SetIterable<S> enabledSymbolsOn(State state);

    boolean available(State state, S symbol);

    SetIterable<State> successorsOf(State state);

    SetIterable<State> successorsOf(State state, S symbol);

    default State successorOf(State state, S symbol)
    {
        throw new UnsupportedOperationException("only available on deterministic instances");
    }

    SetIterable<State> predecessorsOf(State state);

    SetIterable<State> predecessorsOf(State state, S symbol);

    default SetIterable<State> epsilonClosureOf(SetIterable<State> states)
    {
        throw new UnsupportedOperationException("only available on nondeterministic instances");
    }

    default SetIterable<State> epsilonClosureOf(SetIterable<State> states, S symbol)
    {
        throw new UnsupportedOperationException("only available on nondeterministic instances");
    }

    @Override
    String toString();

    interface Builder<S>
    {
        Builder<S> removeState(State state);

        Builder<S> addTransition(State dept, State dest, S symbol);

        Builder<S> removeTransition(State dept, State dest, S symbol);

        DeltaFunction<S> build();

        DeltaFunction<S> build(boolean generalized);
    }
}
