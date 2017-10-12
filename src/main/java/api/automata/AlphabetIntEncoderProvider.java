package api.automata;

import org.eclipse.collections.api.list.MutableList;

public interface AlphabetIntEncoderProvider
{
    <S> AlphabetIntEncoder<S> create(MutableList<S> definition, S epsilon);

    <S> AlphabetIntEncoder<S> create(Alphabet<S> alphabet);
}
