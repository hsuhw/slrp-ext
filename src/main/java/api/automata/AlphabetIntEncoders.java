package api.automata;

import org.eclipse.collections.api.list.MutableList;

import java.util.ServiceLoader;

import static api.automata.AlphabetIntEncoder.Provider;

public final class AlphabetIntEncoders
{
    private AlphabetIntEncoders()
    {
    }

    public static <S> AlphabetIntEncoder<S> create(MutableList<S> definition, S epsilon)
    {
        return Singleton.INSTANCE.create(definition, epsilon);
    }

    public static <S> AlphabetIntEncoder<S> create(Alphabet<S> alphabet)
    {
        return Singleton.INSTANCE.create(alphabet);
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
