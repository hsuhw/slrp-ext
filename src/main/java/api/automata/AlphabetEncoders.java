package api.automata;

import core.automata.BiMapAlphabetEncoder;
import org.eclipse.collections.api.bimap.MutableBiMap;

public final class AlphabetEncoders
{
    private AlphabetEncoders()
    {
    }

    public static <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon)
    {
        return new BiMapAlphabetEncoder<>(definition, originEpsilon);
    }
}
