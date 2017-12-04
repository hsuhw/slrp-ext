package core.automata;

import api.automata.TransitionGraph;

import static api.automata.TransitionGraph.Builder;

public final class BasicGraphProvider implements TransitionGraph.Provider
{
    @Override
    public <N, A> Builder<N, A> builder(int nodeCapacity, int arcCapacity, A epsilonLabel)
    {
        return new MapMapSetGraphBuilder<>(nodeCapacity, arcCapacity, epsilonLabel);
    }

    @Override
    public <N, A> Builder<N, A> builder(TransitionGraph<N, A> base)
    {
        return new MapMapSetGraphBuilder<>((MapMapSetGraph<N, A>) base);
    }

    @Override
    public <N, A> Builder<N, A> builder(TransitionGraph<N, A> base, int nodeCapacity, int arcCapacity)
    {
        return new MapMapSetGraphBuilder<>((MapMapSetGraph<N, A>) base, nodeCapacity, arcCapacity);
    }
}
