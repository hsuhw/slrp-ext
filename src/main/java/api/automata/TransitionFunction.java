package api.automata;

import org.eclipse.collections.api.set.ImmutableSet;

public interface TransitionFunction<S extends Symbol>
{
    int size();

    ImmutableSet<S> enabledSymbolsOn(State state);

    ImmutableSet<State> successorsOf(State state);

    ImmutableSet<State> successorsOf(State state, S symbol);

    default State successorOf(State state, S symbol)
    {
        throw new UnsupportedOperationException("only available on deterministic instances");
    }

    ImmutableSet<State> predecessorsOf(State state);

    ImmutableSet<State> predecessorsOf(State state, S symbol);

    @Override
    String toString();
}
