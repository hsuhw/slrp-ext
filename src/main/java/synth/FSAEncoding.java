package synth;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;

interface FSAEncoding
{
    void ensureDeterminism();

    void ensureNoDanglingStates();

    void ensureNoDeadEndStates();

    void ensureAcceptWord(ImmutableIntList word);

    void ensureNotAcceptWord(ImmutableIntList word);

    void whetherAcceptWord(int indicator, ImmutableIntList word);
}
