package api.automata;

import core.automata.MapListAlphabetIntEncoder;
import org.eclipse.collections.api.list.MutableList;

public final class AlphabetIntEncoders
{
    private AlphabetIntEncoders()
    {
    }

    public static <S> AlphabetIntEncoder<S> create(MutableList<S> definition, S epsilon)
    {
        return new MapListAlphabetIntEncoder<>(definition, epsilon);
    }
}
