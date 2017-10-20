package core.automata;

import api.automata.AlphabetEncoder;
import org.eclipse.collections.api.bimap.MutableBiMap;

import static api.automata.AlphabetEncoder.Builder;

public final class BasicAlphabetEncoders implements AlphabetEncoder.Provider
{
    public <S, T> Builder<S, T> builder(int sizeEstimate, S originEpsilon, T targetEpsilon)
    {
        return new BiMapAlphabetEncoderBuilder<>(sizeEstimate, originEpsilon, targetEpsilon);
    }

    public <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon)
    {
        return new BiMapAlphabetEncoder<>(definition, originEpsilon);
    }
}