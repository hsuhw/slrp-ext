package api.automata;

import org.eclipse.collections.api.bimap.MutableBiMap;

public interface AlphabetEncoderProvider
{
    <S, T> AlphabetEncoder.Builder<S, T> builder(int sizeEstimate);

    <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon);
}
