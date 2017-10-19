package core.automata;

import api.automata.TransitionGraph;

import static api.automata.TransitionGraph.Builder;

public final class BasicGraphs implements TransitionGraph.Provider
{
    @Override
    public <N, A> Builder<N, A> builder(int nodeCapacity, int arcCapacity, A epsilonLabel)
    {
        return new MapMapSetGraphBuilder<>(nodeCapacity, arcCapacity, epsilonLabel);
    }

    @Override
    public <N, A> Builder<N, A> builderOn(TransitionGraph<N, A> graph)
    {
        return new MapMapSetGraphBuilder<>((MapMapSetGraph<N, A>) graph);
    }
}
