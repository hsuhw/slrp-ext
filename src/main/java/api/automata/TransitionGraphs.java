package api.automata;

import java.util.ServiceLoader;

import static api.automata.TransitionGraph.Builder;
import static api.automata.TransitionGraph.Provider;

public final class TransitionGraphs
{
    private TransitionGraphs()
    {
    }

    public static <N, A> Builder<N, A> builder(int nodeCapacity, int arcCapacity, A epsilonLabel)
    {
        return Singleton.INSTANCE.builder(nodeCapacity, arcCapacity, epsilonLabel);
    }

    public static <N, A> Builder<N, A> builder(TransitionGraph<N, A> graph)
    {
        return Singleton.INSTANCE.builder(graph);
    }

    public static <N, A> Builder<N, A> builder(TransitionGraph<N, A> graph, int nodeCapacity, int arcCapacity)
    {
        return Singleton.INSTANCE.builder(graph, nodeCapacity, arcCapacity);
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
