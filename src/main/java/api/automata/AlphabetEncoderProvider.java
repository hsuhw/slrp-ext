package api.automata;

import org.eclipse.collections.api.bimap.MutableBiMap;

import static api.automata.AlphabetEncoder.Builder;

public interface AlphabetEncoderProvider
{
    <S, T> Builder<S, T> builder(int sizeEstimate);

    <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon);
}
