package api.automata;

import org.eclipse.collections.api.list.MutableList;

import java.util.ServiceLoader;

public final class AlphabetIntEncoders
{
    private AlphabetIntEncoders()
    {
    }

    public static <S> AlphabetIntEncoder<S> create(MutableList<S> definition, S epsilon)
    {
        return Provider.INSTANCE.create(definition, epsilon);
    }

    public static <S> AlphabetIntEncoder<S> create(Alphabet<S> alphabet)
    {
        return Provider.INSTANCE.create(alphabet);
    }

    private static final class Provider // Bill Pugh singleton pattern
    {
        private static final AlphabetIntEncoderProvider INSTANCE;

        static {
            ServiceLoader<AlphabetIntEncoderProvider> loader = ServiceLoader.load(AlphabetIntEncoderProvider.class);
            INSTANCE = loader.stream().reduce((former, latter) -> latter) // get the last provider in classpath
                             .orElseThrow(IllegalStateException::new).get();
        }
    }
}
