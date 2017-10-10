package api.automata;

import core.automata.BiMapAlphabetEncoder;
import core.automata.BiMapAlphabetEncoderBuilder;
import org.eclipse.collections.api.bimap.MutableBiMap;

import static api.automata.AlphabetEncoder.Builder;

public final class AlphabetEncoders
{
    private AlphabetEncoders()
    {
    }

    public static <O, T> Builder<O, T> builder(int sizeEstimate)
    {
        return new BiMapAlphabetEncoderBuilder<>(sizeEstimate);
    }

    public static <O, T> AlphabetEncoder<O, T> create(MutableBiMap<O, T> definition, O originEpsilon)
    {
        return new BiMapAlphabetEncoder<>(definition, originEpsilon);
    }
}
