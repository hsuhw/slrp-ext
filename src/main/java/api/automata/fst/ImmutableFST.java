package api.automata.fst;

import api.automata.ImmutableAutomaton;
import org.eclipse.collections.api.tuple.Pair;

public interface ImmutableFST<S, T> extends ImmutableAutomaton<Pair<S, T>>, FST<S, T>
{
    @Override
    default ImmutableFST<S, T> toImmutable()
    {
        return this;
    }
}
