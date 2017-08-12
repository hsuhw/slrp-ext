package api.synth;

import api.automata.Deterministic;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

public interface FSAEncoding<S extends Symbol> extends Deterministic
{
    void ensureNoUnreachableStates();

    void ensureNoDeadEndStates();

    void ensureAcceptingWord(ImmutableList<S> word);

    void ensureNotAcceptingWord(ImmutableList<S> word);

    void whetherAcceptWord(int indicator, ImmutableList<S> word);

    void ensureNoWordsPurelyMadeOf(ImmutableSet<S> symbols);

    void blockCurrentInstance();

    FSA<S> toFSA();
}
