package synth;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;

interface FSAEncoding
{
    void ensureDeterminism();

    void ensureNoDanglingStates();

    void ensureNoDeadEndStates();

    void ensureAcceptingWord(ImmutableIntList word);
}
