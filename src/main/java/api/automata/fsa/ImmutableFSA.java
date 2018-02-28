package api.automata.fsa;

import api.automata.ImmutableAutomaton;
import api.automata.ImmutableState;

public interface ImmutableFSA<S extends ImmutableState<T>, T> extends ImmutableAutomaton<S, T>, FSA<S, T>
{
    @Override
    default ImmutableFSA<S, T> toImmutable()
    {
        return this;
    }
}
