package core.automata;

import api.automata.TransitionGraph;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;

public class MapMapSetGraph<N, A> implements TransitionGraph<N, A>
{
    private final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> forwardGraph;
    private final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> backwardGraph;
    private final A epsilonLabel;

    private ImmutableSet<N> nodes;
    private ImmutableSet<A> arcLabels;
    private Boolean arcDeterministic;

    private MapMapSetGraph(ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> forwardDefinition,
                           ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> backwardDefinition, A epsilonLabel)
    {
        forwardGraph = forwardDefinition;
        backwardGraph = backwardDefinition;
        this.epsilonLabel = epsilonLabel;
    }

    public MapMapSetGraph(MapMapSetGraphBuilder<N, A> builder)
    {
        this(builder.forwardGraph(), builder.backwardGraph(), builder.epsilonLabel());
    }

    @Override
    public int size()
    {
        return (int) forwardGraph.collectLong(arcRecord -> arcRecord.collectInt(RichIterable::size).sum()).sum();
    }

    @Override
    public ImmutableSet<N> referredNodes()
    {
        if (nodes == null) {
            nodes = Sets.union(forwardGraph.keysView().toSet(), backwardGraph.keysView().toSet()).toImmutable();
        }

        return nodes;
    }

    @Override
    public ImmutableSet<A> referredArcLabels()
    {
        if (arcLabels == null) {
            arcLabels = forwardGraph.flatCollect(MapIterable::keysView).toSet().toImmutable();
        }

        return arcLabels;
    }

    @Override
    public A epsilonLabel()
    {
        return epsilonLabel;
    }

    private boolean noEpsilonTransAndOnlyOneDest(ImmutableMap<A, ImmutableSet<N>> arcRecord)
    {
        return !arcRecord.containsKey(epsilonLabel) && arcRecord.allSatisfy(dests -> dests.size() == 1);
    }

    @Override
    public boolean arcDeterministic()
    {
        if (arcDeterministic == null) {
            arcDeterministic = forwardGraph.allSatisfy(this::noEpsilonTransAndOnlyOneDest);
        }

        return arcDeterministic;
    }

    @Override
    public SetIterable<A> enabledArcsOn(N node)
    {
        return forwardGraph.containsKey(node) ? forwardGraph.get(node).keysView().toSet() // one-off
                                              : Sets.immutable.empty();
    }

    @Override
    public boolean hasSomeArc(N node, A arc)
    {
        return forwardGraph.containsKey(node) && forwardGraph.get(node).get(arc) != null;
    }

    private SetIterable<N> successorsOfFrom(ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph, N node)
    {
        return graph.containsKey(node) ? graph.get(node).flatCollect(x -> x).toSet() // one-off
                                       : Sets.immutable.empty();
    }

    @Override
    public SetIterable<N> successorsOf(N node)
    {
        return successorsOfFrom(forwardGraph, node);
    }

    private ImmutableSet<N> successorsOfFrom(ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph, N node, A arc)
    {
        return graph.containsKey(node) && graph.get(node).containsKey(arc)
               ? graph.get(node).get(arc)
               : Sets.immutable.empty();
    }

    @Override
    public ImmutableSet<N> successorsOf(N node, A arc)
    {
        return successorsOfFrom(forwardGraph, node, arc);
    }

    @Override
    public SetIterable<N> predecessorsOf(N node)
    {
        return successorsOfFrom(backwardGraph, node);
    }

    @Override
    public ImmutableSet<N> predecessorsOf(N node, A arc)
    {
        return successorsOfFrom(backwardGraph, node, arc);
    }

    final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> forwardGraph()
    {
        return forwardGraph;
    }

    final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> backwardGraph()
    {
        return backwardGraph;
    }
}
