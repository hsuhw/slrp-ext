package api.automata;

import java.util.ServiceLoader;

import static api.automata.State.Provider;

public final class States
{
    private States()
    {
    }

    public static State create(String name)
    {
        return Singleton.INSTANCE.create(name);
    }

    public static State generate()
    {
        return Singleton.INSTANCE.generate();
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
