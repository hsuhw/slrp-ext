package core.automata;

import api.automata.TransitionGraph;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.util.Values.*;
import static api.util.Values.Direction.BACKWARD;
import static api.util.Values.Direction.FORWARD;

public class MapMapSetGraph<N, A> implements TransitionGraph<N, A>
{
    private final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> forwardGraph;
    private final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> backwardGraph;
    private final A epsilonLabel;

    private int size = -1;
    private ImmutableSet<N> nodes;
    private ImmutableSet<A> arcLabels;
    private Boolean arcDeterministic;
    private String defaultDisplay;

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
        if (size == -1) {
            size = (int) forwardGraph.collectLong(arcs -> arcs.collectInt(ImmutableSet::size).sum()).sum();
        }

        return size;
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
            arcLabels = forwardGraph.flatCollect(ImmutableMap::keysView).toSet().toImmutable();
        }

        return arcLabels;
    }

    @Override
    public SetIterable<Pair<A, N>> arcsFrom(N node)
    {
        if (!forwardGraph.containsKey(node)) {
            return Sets.immutable.empty();
        }

        return forwardGraph.get(node).keyValuesView().flatCollect(each -> {
            final A label = each.getOne();
            final ImmutableSet<N> dests = each.getTwo();
            return dests.collect(dest -> Tuples.pair(label, dest));
        }).toSet(); // one-off
    }

    private SetIterable<A> arcLabelsFrom(N node, Direction dir)
    {
        final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

        return graph.containsKey(node) ? graph.get(node).keysView().toSet() // one-off
                                       : Sets.immutable.empty();
    }

    @Override
    public SetIterable<A> arcLabelsFrom(N node)
    {
        return arcLabelsFrom(node, FORWARD);
    }

    private boolean arcLabeledFrom(N node, A arcLabel, Direction dir)
    {
        final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

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
            final ImmutableSet<N> dests = each.getTwo();
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

    private SetIterable<N> directSuccessorsOf(N node, Direction dir)
    {
        final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;
        final MutableSet<N> set = UnifiedSet.newSet(referredNodes().size()); // upper bound

        return graph.containsKey(node) ? graph.get(node).flatCollect(x -> x, set) // one-off
                                       : Sets.immutable.empty();
    }

    @Override
    public SetIterable<N> directSuccessorsOf(N node)
    {
        return directSuccessorsOf(node, FORWARD);
    }

    private ImmutableSet<N> directSuccessorsOf(N node, A arcLabel, Direction dir)
    {
        final ImmutableMap<N, ImmutableMap<A, ImmutableSet<N>>> graph = dir == FORWARD ? forwardGraph : backwardGraph;

        return graph.containsKey(node) && graph.get(node).containsKey(arcLabel)
               ? graph.get(node).get(arcLabel)
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
    public A epsilonLabel()
    {
        return epsilonLabel;
    }

    private boolean noEpsilonArcsAndOnlyOneDest(ImmutableMap<A, ImmutableSet<N>> arcs)
    {
        return !arcs.containsKey(epsilonLabel) && arcs.allSatisfy(dests -> dests.size() == 1);
    }

    @Override
    public boolean arcDeterministic()
    {
        if (arcDeterministic == null) {
            arcDeterministic = forwardGraph.allSatisfy(this::noEpsilonArcsAndOnlyOneDest);
        }

        return arcDeterministic;
    }

    @Override
    public String toString()
    {
        if (defaultDisplay == null) {
            defaultDisplay = toStringWith(Maps.immutable.empty());
        }

        return defaultDisplay;
    }

    @Override
    public String toStringWith(ImmutableMap<N, String> nodeNames)
    {
        final StringBuilder layout = new StringBuilder();
        forwardGraph.forEachKeyValue((dept, arcRecord) -> {
            final String deptName = nodeNames.containsKey(dept) ? nodeNames.get(dept) : dept.toString();
            arcRecord.forEachKeyValue((label, dests) -> {
                dests.forEach(dest -> {
                    final String destName = nodeNames.containsKey(dest) ? nodeNames.get(dest) : dest.toString();
                    layout.append(DISPLAY_INDENT);
                    layout.append(deptName).append(" -> ").append(destName).append(" [").append(label).append("];");
                    layout.append(DISPLAY_NEWLINE);
                });
            });
        });

        return layout.toString();
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
