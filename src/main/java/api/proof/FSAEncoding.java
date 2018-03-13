package api.proof;

import api.automata.Alphabet;
import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

public interface FSAEncoding<S>
{
    void ensureNoUnreachableState();

    void ensureNoDeadEndState();

    default void ensureNoDanglingState()
    {
        ensureNoUnreachableState();
        ensureNoDeadEndState();
    }

    void ensureAccepting(ListIterable<S> word);

    void ensureNoAccepting(ListIterable<S> word);

    void ensureAcceptingIfOnlyIf(int indicator, ListIterable<S> word);

    CertainWord<S> ensureAcceptingCertainWordIf(int indicator, int length);

    void ensureNoWordPurelyMadeOf(SetIterable<S> symbols);

    void blockCurrentInstance();

    FSA<S> resolve();

    interface CertainWord<S>
    {
        int length();

        Alphabet<S> alphabet();

        int getCharacterIndicator(int pos, S symbol);

        void setCharacterAt(int pos, S symbol);

        void ensureAcceptedBy(FSA<S> fsa);
    }
}
