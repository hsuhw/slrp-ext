package api.automata;

import org.eclipse.collections.api.bimap.MutableBiMap;

import java.util.ServiceLoader;

import static api.automata.AlphabetEncoder.Builder;
import static api.automata.AlphabetEncoder.Provider;

public final class AlphabetEncoders
{
    private AlphabetEncoders()
    {
    }

    public static <S, T> Builder<S, T> builder(int sizeEstimate, S originEpsilon, T targetEpsilon)
    {
        return Singleton.INSTANCE.builder(sizeEstimate, originEpsilon, targetEpsilon);
    }

    public static <S, T> AlphabetEncoder<S, T> create(MutableBiMap<S, T> definition, S originEpsilon)
    {
        return Singleton.INSTANCE.create(definition, originEpsilon);
    }

    private static final class Singleton
    {
        private static final Provider INSTANCE;

        static {
            ServiceLoader<Provider> loader = ServiceLoader.load(Provider.class);
            INSTANCE = loader.stream().reduce((former, latter) -> latter) // get the last provider in classpath
                             .orElseThrow(IllegalStateException::new).get();
        }
    }
}
