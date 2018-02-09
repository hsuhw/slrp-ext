package common;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public interface Digraph<N, A>
{
    ImmutableSet<N> referredNodes();

    ImmutableSet<A> referredArcLabels();

    SetIterable<Pair<A, N>> arcsFrom(N node);

    SetIterable<A> arcLabelsFrom(N node);

    boolean arcLabeledFrom(N node, A arcLabel);

    SetIterable<Pair<N, A>> arcsTo(N node);

    SetIterable<A> arcLabelsTo(N node);

    boolean arcLabeledTo(N node, A arcLabel);

    SetIterable<A> arcLabelsOn(N from, N to);

    SetIterable<N> directSuccessorsOf(N node);

    ImmutableSet<N> directSuccessorsOf(N node, A arcLabel);

    default SetIterable<N> directSuccessorsOf(SetIterable<N> nodes, A arcLabel)
    {
        final MutableSet<N> set = UnifiedSet.newSet(referredNodes().size()); // upper bound

        return nodes.flatCollect(state -> directSuccessorsOf(state, arcLabel), set); // one-off
    }

    SetIterable<N> directPredecessorsOf(N node);

    ImmutableSet<N> directPredecessorsOf(N node, A arcLabel);

    default SetIterable<N> directPredecessorsOf(SetIterable<N> nodes, A arcLabel)
    {
        final MutableSet<N> set = UnifiedSet.newSet(referredNodes().size()); // upper bound

        return nodes.flatCollect(state -> directPredecessorsOf(state, arcLabel), set); // one-off
    }

    @Override
    String toString();
}
