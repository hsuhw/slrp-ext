package core.automata;

import api.automata.AlphabetEncoder;
import api.automata.AlphabetEncoderProvider;
import org.eclipse.collections.api.bimap.MutableBiMap;

import static api.automata.AlphabetEncoder.Builder;

public final class AlphabetEncoders implements AlphabetEncoderProvider
{
    public <S, T> Builder<S, T> builder(int sizeEstimate)
    {
        return new BiMapAlphabetEncoderBuilder<>(sizeEstimate);
    }

    public <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon)
    {
        return new BiMapAlphabetEncoder<>(definition, originEpsilon);
    }
}
