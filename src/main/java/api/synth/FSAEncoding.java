package api.synth;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

public interface FSAEncoding<S>
{
    void ensureNoUnreachableState();

    void ensureNoDeadEndState();

    void ensureAcceptingWord(ImmutableList<S> word);

    void ensureNotAcceptingWord(ImmutableList<S> word);

    void whetherAcceptWord(int indicator, ImmutableList<S> word);

    void ensureNoWordPurelyMadeOf(ImmutableSet<S> symbols);

    void blockCurrentInstance();

    FSA<S> resolve() throws SatSolverTimeoutException;
}
