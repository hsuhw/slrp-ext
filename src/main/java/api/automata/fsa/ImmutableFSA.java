package api.automata.fsa;

import api.automata.ImmutableAutomaton;

public interface ImmutableFSA<S> extends ImmutableAutomaton<S>, FSA<S>
{
    @Override
    default ImmutableFSA<S> toImmutable()
    {
        return this;
    }
}
