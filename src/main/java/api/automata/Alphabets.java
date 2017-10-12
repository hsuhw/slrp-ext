package api.automata;

import org.eclipse.collections.api.set.MutableSet;

import java.util.ServiceLoader;

import static api.automata.Alphabet.Builder;

public final class Alphabets
{
    private Alphabets()
    {
    }

    public static <S> Builder<S> builder(int sizeEstimate, S epsilon)
    {
        return Provider.INSTANCE.builder(sizeEstimate, epsilon);
    }

    public static <S> Builder<S> builderBasedOn(Alphabet<S> alphabet)
    {
        return Provider.INSTANCE.builderBasedOn(alphabet);
    }

    public static <S> Alphabet<S> create(MutableSet<S> definition, S epsilon)
    {
        return Provider.INSTANCE.create(definition, epsilon);
    }

    private static final class Provider // Bill Pugh singleton pattern
    {
        private static final AlphabetProvider INSTANCE;

        static {
            ServiceLoader<AlphabetProvider> loader = ServiceLoader.load(AlphabetProvider.class);
            INSTANCE = loader.stream().reduce((former, latter) -> latter) // get the last provider in classpath
                             .orElseThrow(IllegalStateException::new).get();
        }
    }
}
