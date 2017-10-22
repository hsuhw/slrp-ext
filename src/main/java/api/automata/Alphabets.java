package api.automata;

import org.eclipse.collections.api.set.MutableSet;

import java.util.ServiceLoader;

import static api.automata.Alphabet.Builder;
import static api.automata.Alphabet.Provider;

public final class Alphabets
{
    private Alphabets()
    {
    }

    public static <S> Builder<S> builder(int sizeEstimate, S epsilon)
    {
        return Singleton.INSTANCE.builder(sizeEstimate, epsilon);
    }

    public static <S> Builder<S> builderOn(Alphabet<S> alphabet)
    {
        return Singleton.INSTANCE.builderOn(alphabet);
    }

    public static <S> Alphabet<S> create(MutableSet<S> definition, S epsilon)
    {
        return Singleton.INSTANCE.create(definition, epsilon);
    }

    private static final class Singleton
    {
        private static final Provider INSTANCE;

        static {
            ServiceLoader<Provider> loader = ServiceLoader.load(Provider.class);
            INSTANCE = loader.stream().reduce((__, latter) -> latter) // get the last provider in classpath
                             .orElseThrow(IllegalStateException::new).get();
        }
    }
}
