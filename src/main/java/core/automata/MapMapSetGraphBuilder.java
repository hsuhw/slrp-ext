package core.automata;

import api.automata.TransitionGraph;
import core.util.Assertions;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.TransitionGraph.Builder;
import static core.util.Parameters.NONDETERMINISTIC_TRANSITION_CAPACITY;
import static core.util.Parameters.estimateExtendedSize;

public class MapMapSetGraphBuilder<N, A> implements TransitionGraph.Builder<N, A>
{
    private final int arcCapacity;
    private final MutableMap<N, MutableMap<A, MutableSet<N>>> forwardGraph;
    private final MutableMap<N, MutableMap<A, MutableSet<N>>> backwardGraph;
    private final A epsilonLabel;

    public MapMapSetGraphBuilder(int nodeCapacity, int arcCapacity, A epsilonLabel)
    {
        Assertions.argumentNotNull(epsilonLabel);
        if (arcCapacity < 0) {
            throw new IllegalArgumentException("capacity cannot be less than 0");
        }

        forwardGraph = UnifiedMap.newMap(nodeCapacity);
        backwardGraph = UnifiedMap.newMap(nodeCapacity);
        this.arcCapacity = arcCapacity;
        this.epsilonLabel = epsilonLabel;
    }

    private static <N> MutableSet<N> newNodeSet()
    {
        return UnifiedSet.newSet(NONDETERMINISTIC_TRANSITION_CAPACITY);
    }

    private static <N, A> MutableMap<N, MutableMap<A, MutableSet<N>>> toMutable(
        ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph, int nodeCapacity, int arcCapacity)
    {
        final MutableMap<N, MutableMap<A, MutableSet<N>>> result = UnifiedMap.newMap(nodeCapacity);
        graph.forEachKeyValue((dept, arcRecord) -> {
            final MutableMap<A, MutableSet<N>> record = UnifiedMap.newMap(arcCapacity);
            arcRecord.forEachKeyValue((arcLabel, dests) -> {
                final MutableSet<N> nodes = newNodeSet();
                nodes.addAllIterable(dests);
                record.put(arcLabel, nodes);
            });
            result.put(dept, record);
        });

        return result;
    }

    public MapMapSetGraphBuilder(MapMapSetGraph<N, A> graph)
    {
        final int nodeCapacity = estimateExtendedSize(graph.referredNodes().size());
        arcCapacity = estimateExtendedSize(graph.referredArcLabels().size());
        forwardGraph = toMutable(graph.forwardGraph(), nodeCapacity, arcCapacity);
        backwardGraph = toMutable(graph.backwardGraph(), nodeCapacity, arcCapacity);
        epsilonLabel = graph.epsilonLabel();
    }

    private <N, A> MutableMap<A, MutableSet<N>> newArcRecord()
    {
        return UnifiedMap.newMap(arcCapacity);
    }

    @Override
    public int currentSize()
    {
        return (int) forwardGraph.collectLong(arcRecord -> arcRecord.collectInt(MutableSet::size).sum()).sum();
    }

    @Override
    public boolean isEmpty()
    {
        return forwardGraph.isEmpty();
    }

    @Override
    public Builder<N, A> addArc(N from, N to, A arcLabel)
    {
        Assertions.argumentNotNull(from, to, arcLabel);
        if (from.equals(to) && arcLabel.equals(epsilonLabel)) {
            return this;
        }

        forwardGraph.computeIfAbsent(from, __ -> newArcRecord()) // estimate given
                    .computeIfAbsent(arcLabel, __ -> newNodeSet()) // global parameter
                    .add(to);
        backwardGraph.computeIfAbsent(to, __ -> newArcRecord()) // estimate given
                     .computeIfAbsent(arcLabel, __ -> newNodeSet()) // global parameter
                     .add(from);

        return this;
    }

    private void removeArcFrom(MutableMap<N, MutableMap<A, MutableSet<N>>> graph, N from, N to, A arcLabel)
    {
        final MutableMap<A, MutableSet<N>> arcRecord = graph.get(from);
        final MutableSet<N> dests = arcRecord.get(arcLabel);
        dests.remove(to);
        if (dests.isEmpty()) {
            arcRecord.remove(arcLabel);
        }
        if (arcRecord.isEmpty()) {
            graph.remove(from);
        }
    }

    @Override
    public Builder<N, A> removeArc(N from, N to, A arcLabel)
    {
        Assertions.argumentNotNull(from, to, arcLabel);

        removeArcFrom(forwardGraph, from, to, arcLabel);
        removeArcFrom(backwardGraph, to, from, arcLabel);

        return this;
    }

    @Override
    public Builder<N, A> removeNode(N node)
    {
        Assertions.argumentNotNull(node);

        if (forwardGraph.containsKey(node)) {
            forwardGraph.get(node).forEach((arc, dests) -> {
                dests.forEach(dest -> removeArcFrom(backwardGraph, dest, node, arc));
            });
            forwardGraph.remove(node);
        }
        if (backwardGraph.containsKey(node)) {
            backwardGraph.get(node).forEach((arc, depts) -> {
                depts.forEach(dept -> removeArcFrom(forwardGraph, dept, node, arc));
            });
            backwardGraph.remove(node);
        }

        return this;
    }

    @Override
    public MapMapSetGraph<N, A> build()
    {
        return new MapMapSetGraph<>(this);
    }

    private ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> toImmutable(
        MutableMap<N, MutableMap<A, MutableSet<N>>> graph)
    {
        return graph.collect((dept, arcRecord) -> {
            return Tuples.pair(dept, arcRecord.collect((arcLabel, dests) -> {
                return Tuples.pair(arcLabel, dests.toImmutable());
            }).toImmutable());
        }).toImmutable();
    }

    final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> forwardGraph()
    {
        return toImmutable(forwardGraph);
    }

    final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> backwardGraph()
    {
        return toImmutable(backwardGraph);
    }

    final A epsilonLabel()
    {
        return epsilonLabel;
    }
}
