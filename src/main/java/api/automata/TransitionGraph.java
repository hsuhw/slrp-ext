package api.automata;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static core.util.Parameters.estimateExtendedSize;

public interface TransitionGraph<N, A>
{
    int size();

    default boolean isEmpty()
    {
        return size() == 0;
    }

    ImmutableSet<N> referredNodes();

    ImmutableSet<A> referredArcLabels();

    A epsilonLabel();

    boolean arcDeterministic();

    SetIterable<A> enabledArcsOn(N node);

    ImmutableSet<A> nonEpsilonArcsOn(N node);

    boolean hasArc(N node, A arcLabel);

    SetIterable<N> successorsOf(N node);

    ImmutableSet<N> successorsOf(N node, A arcLabel);

    default SetIterable<N> successorsOf(SetIterable<N> nodes, A arcLabel)
    {
        return nodes.flatCollect(state -> successorsOf(state, arcLabel)).toSet(); // one-off
    }

    default N successorOf(N node, A arcLabel)
    {
        if (!arcDeterministic() || arcLabel.equals(epsilonLabel())) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        return successorsOf(node, arcLabel).getOnly();
    }

    SetIterable<N> predecessorsOf(N node);

    ImmutableSet<N> predecessorsOf(N node, A arcLabel);

    default SetIterable<N> predecessorsOf(SetIterable<N> nodes, A arcLabel)
    {
        return nodes.flatCollect(state -> predecessorsOf(state, arcLabel)).toSet(); // one-off
    }

    default SetIterable<N> epsilonClosureOf(SetIterable<N> nodes)
    {
        if (arcDeterministic()) {
            throw new UnsupportedOperationException("only available on nondeterministic instances");
        }

        return epsilonClosureOf(nodes, epsilonLabel());
    }

    default SetIterable<N> epsilonClosureOf(SetIterable<N> nodes, A arcLabel)
    {
        if (arcDeterministic()) {
            throw new UnsupportedOperationException("only available on nondeterministic instances");
        }

        MutableSet<N> base = UnifiedSet.newSet(estimateExtendedSize(nodes.size())); // heuristic
        base.addAllIterable(nodes);
        MutableSet<N> curr;
        while (!base.containsAll((curr = successorsOf(base, epsilonLabel()).toSet()))) {
            base.addAllIterable(curr);
        }

        return arcLabel.equals(epsilonLabel()) ? base : epsilonClosureOf(successorsOf(base, arcLabel));
    }

    @Override
    String toString();

    String toString(ImmutableMap<N, String> nodeNameOverride);

    interface Builder<N, A>
    {
        int currentSize();

        boolean isEmpty();

        Builder<N, A> addArc(N from, N to, A arcLabel);

        Builder<N, A> removeArc(N from, N to, A arcLabel);

        Builder<N, A> removeNode(N node);

        TransitionGraph<N, A> build();
    }

    interface Provider
    {
        <N, A> Builder<N, A> builder(int nodeCapacity, int arcCapacity, A epsilonLabel);

        <N, A> Builder<N, A> builderOn(TransitionGraph<N, A> graph);
    }
}
