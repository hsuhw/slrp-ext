package api.automata;

import org.eclipse.collections.api.bimap.MutableBiMap;

import java.util.ServiceLoader;

import static api.automata.AlphabetEncoder.Builder;

public final class AlphabetEncoders
{
    private AlphabetEncoders()
    {
    }

    public static <S, T> Builder<S, T> builder(int sizeEstimate)
    {
        return Provider.INSTANCE.builder(sizeEstimate);
    }

    public static <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon)
    {
        return Provider.INSTANCE.create(definition, originEpsilon);
    }

    private static final class Provider // Bill Pugh singleton pattern
    {
        private static final AlphabetEncoderProvider INSTANCE;

        static {
            ServiceLoader<AlphabetEncoderProvider> loader = ServiceLoader.load(AlphabetEncoderProvider.class);
            INSTANCE = loader.stream().reduce((former, latter) -> latter) // get the last provider in classpath
                             .orElseThrow(IllegalStateException::new).get();
        }
    }
}
