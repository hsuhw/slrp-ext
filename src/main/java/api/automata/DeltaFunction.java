package api.automata;

import org.eclipse.collections.api.set.SetIterable;

public interface DeltaFunction<S>
{
    int size();

    SetIterable<State> getAllReferredStates();

    SetIterable<S> getAllReferredSymbols();

    SetIterable<S> enabledSymbolsOn(State state);

    SetIterable<State> successorsOf(State state);

    SetIterable<State> successorsOf(State state, S symbol);

    default State successorOf(State state, S symbol)
    {
        throw new UnsupportedOperationException("only available on deterministic instances");
    }

    SetIterable<State> predecessorsOf(State state);

    SetIterable<State> predecessorsOf(State state, S symbol);

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
