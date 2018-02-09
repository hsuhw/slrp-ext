package core.automata;

import api.automata.TransitionGraph;
import api.util.Values;
import common.Digraph;
import common.util.Assert;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.TransitionGraph.Builder;
import static api.util.Values.Direction.BACKWARD;
import static api.util.Values.Direction.FORWARD;
import static core.Parameters.NONDETERMINISTIC_TRANSITION_CAPACITY;
import static core.Parameters.estimateExtendedSize;

public class MapMapSetGraphBuilder<N, A> implements TransitionGraph.Builder<N, A>, Digraph<N, A>
{
    private final int arcCapacity;
    private final MutableMap<N, MutableMap<A, MutableSet<N>>> forwardGraph;
    private final MutableMap<N, MutableMap<A, MutableSet<N>>> backwardGraph;
    private final A epsilonLabel;

    public MapMapSetGraphBuilder(int nodeCapacity, int arcCapacity, A epsilonLabel)
    {
        Assert.argumentNotNull(epsilonLabel);
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

    public MapMapSetGraphBuilder(MapMapSetGraph<N, A> graph, int nodeCapacity, int arcCapacity)
    {
        this.arcCapacity = arcCapacity;
        forwardGraph = toMutable(graph.forwardGraph(), nodeCapacity, arcCapacity);
        backwardGraph = toMutable(graph.backwardGraph(), nodeCapacity, arcCapacity);
        epsilonLabel = graph.epsilonLabel();
    }

    public MapMapSetGraphBuilder(MapMapSetGraph<N, A> graph)
    {
        this(graph, estimateExtendedSize(graph.referredNodes().size()),
             estimateExtendedSize(graph.referredArcLabels().size()));
    }

    private MutableMap<A, MutableSet<N>> newArcRecord()
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
    public final A epsilonLabel()
    {
        return epsilonLabel;
    }

    @Override
    public Builder<N, A> addArc(N from, N to, A arcLabel)
    {
        Assert.argumentNotNull(from, to, arcLabel);
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
        Assert.argumentNotNull(from, to, arcLabel);

        removeArcFrom(forwardGraph, from, to, arcLabel);
        removeArcFrom(backwardGraph, to, from, arcLabel);

        return this;
    }

    @Override
    public Builder<N, A> removeNode(N node)
    {
        Assert.argumentNotNull(node);

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
    public Digraph<N, A> asGraph()
    {
        return this;
    }

    @Override
    public ImmutableSet<N> referredNodes()
    {
        return Sets.union(forwardGraph.keysView().toSet(), backwardGraph.keysView().toSet()).toImmutable();
    }

    @Override
    public ImmutableSet<A> referredArcLabels()
    {
        return forwardGraph.flatCollect(MutableMap::keysView).toSet().toImmutable();
    }

    @Override
    public SetIterable<Pair<A, N>> arcsFrom(N node)
    {
        if (!forwardGraph.containsKey(node)) {
            return Sets.immutable.empty();
        }

        return forwardGraph.get(node).keyValuesView().flatCollect(each -> {
            final A label = each.getOne();
            final MutableSet<N> dests = each.getTwo();
            return dests.collect(dest -> Tuples.pair(label, dest));
        }).toSet(); // one-off
    }

    private SetIterable<A> arcLabelsFrom(N node, Values.Direction dir)
    {
        final MutableMap<N, MutableMap<A, MutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

        return graph.containsKey(node) ? graph.get(node).keysView().toSet() // one-off
                                       : Sets.immutable.empty();
    }

    @Override
    public SetIterable<A> arcLabelsFrom(N node)
    {
        return arcLabelsFrom(node, FORWARD);
    }

    private boolean arcLabeledFrom(N node, A arcLabel, Values.Direction dir)
    {
        final MutableMap<N, MutableMap<A, MutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

        return graph.containsKey(node) && graph.get(node).containsKey(arcLabel);
    }

    @Override
    public boolean arcLabeledFrom(N node, A arcLabel)
    {
        return arcLabeledFrom(node, arcLabel, FORWARD);
    }

    @Override
    public SetIterable<Pair<N, A>> arcsTo(N node)
    {
        if (!backwardGraph.containsKey(node)) {
            return Sets.immutable.empty();
        }

        return backwardGraph.get(node).keyValuesView().flatCollect(each -> {
            final A label = each.getOne();
            final MutableSet<N> dests = each.getTwo();
            return dests.collect(dest -> Tuples.pair(dest, label));
        }).toSet(); // one-off
    }

    @Override
    public SetIterable<A> arcLabelsTo(N node)
    {
        return arcLabelsFrom(node, BACKWARD);
    }

    @Override
    public boolean arcLabeledTo(N node, A arcLabel)
    {
        return arcLabeledFrom(node, arcLabel, BACKWARD);
    }

    @Override
    public SetIterable<A> arcLabelsOn(N from, N to)
    {
        if (!forwardGraph.containsKey(from)) {
            return Sets.immutable.empty();
        }

        return forwardGraph.get(from).select((label, dests) -> dests.contains(to)).keysView().toSet(); // one-off
    }

    private SetIterable<N> directSuccessorsOf(N node, Values.Direction dir)
    {
        final MutableMap<N, MutableMap<A, MutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

        return graph.containsKey(node) ? graph.get(node).flatCollect(x -> x).toSet() // one-off
                                       : Sets.immutable.empty();
    }

    @Override
    public SetIterable<N> directSuccessorsOf(N node)
    {
        return directSuccessorsOf(node, FORWARD);
    }

    private ImmutableSet<N> directSuccessorsOf(N node, A arcLabel, Values.Direction dir)
    {
        final MutableMap<N, MutableMap<A, MutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

        return graph.containsKey(node) && graph.get(node).containsKey(arcLabel) //
               ? graph.get(node).get(arcLabel).toImmutable() // defense required
               : Sets.immutable.empty();
    }

    @Override
    public ImmutableSet<N> directSuccessorsOf(N node, A arcLabel)
    {
        return directSuccessorsOf(node, arcLabel, FORWARD);
    }

    @Override
    public SetIterable<N> directPredecessorsOf(N node)
    {
        return directSuccessorsOf(node, BACKWARD);
    }

    @Override
    public ImmutableSet<N> directPredecessorsOf(N node, A arcLabel)
    {
        return directSuccessorsOf(node, arcLabel, BACKWARD);
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
}
