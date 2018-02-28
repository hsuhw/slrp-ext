package common;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

public interface Digraph<N, A>
{
    SetIterable<N> referredNodes();

    boolean nodeExists(Object node);

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
        return nodes.flatCollect(state -> directSuccessorsOf(state, arcLabel)).toSet(); // one-off
    }

    SetIterable<N> directPredecessorsOf(N node);

    SetIterable<N> directPredecessorsOf(N node, A arcLabel);

    default SetIterable<N> directPredecessorsOf(SetIterable<N> nodes, A arcLabel)
    {
        return nodes.flatCollect(state -> directPredecessorsOf(state, arcLabel)).toSet(); // one-off
    }

    @Override
    String toString();
}
