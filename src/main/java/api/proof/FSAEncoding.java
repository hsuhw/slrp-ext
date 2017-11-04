package api.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

public interface FSAEncoding<S>
{
    void ensureNoUnreachableState();

    void ensureNoDeadEndState();

    default void ensureNoDanglingState()
    {
        ensureNoUnreachableState();
        ensureNoDeadEndState();
    }

    void ensureAcceptingWord(ImmutableList<S> word);

    void ensureNoAcceptingWord(ImmutableList<S> word);

    void whetherAcceptWord(int indicator, ImmutableList<S> word);

    void ensureNoWordPurelyMadeOf(ImmutableSet<S> symbols);

    void blockCurrentInstance();

    FSA<S> resolve();
}
