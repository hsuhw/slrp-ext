package common;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public interface Digraph<N, A>
{
    SetIterable<N> referredNodes();

    boolean nodeExists(N node);

    SetIterable<A> referredArcLabels();

    RichIterable<? extends Pair<A, N>> arcsFrom(N node);

    SetIterable<A> arcLabelsFrom(N node);

    boolean arcLabeledFrom(N node, A arcLabel);

    RichIterable<Pair<N, A>> arcsTo(N node);

    SetIterable<A> arcLabelsTo(N node);

    boolean arcLabeledTo(N node, A arcLabel);

    SetIterable<A> arcLabelsOn(N from, N to);

    SetIterable<N> directSuccessorsOf(N node);

    SetIterable<N> directSuccessorsOf(N node, A arcLabel);

    default SetIterable<N> directSuccessorsOf(SetIterable<N> nodes, A arcLabel)
    {
        final MutableSet<N> set = UnifiedSet.newSet(referredNodes().size()); // upper bound

        return nodes.flatCollect(state -> directSuccessorsOf(state, arcLabel), set); // one-off
    }

    SetIterable<N> directPredecessorsOf(N node);

    SetIterable<N> directPredecessorsOf(N node, A arcLabel);

    default SetIterable<N> directPredecessorsOf(SetIterable<N> nodes, A arcLabel)
    {
        final MutableSet<N> set = UnifiedSet.newSet(referredNodes().size()); // upper bound

        return nodes.flatCollect(state -> directPredecessorsOf(state, arcLabel), set); // one-off
    }

    @Override
    String toString();
}
