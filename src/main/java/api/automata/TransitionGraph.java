package api.automata;

import api.common.Digraph;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public interface TransitionGraph<N, A> extends Digraph<N, A>
{
    int size();

    default boolean isEmpty()
    {
        return size() == 0;
    }

    A epsilonLabel();

    boolean arcDeterministic();

    default ImmutableSet<A> nonEpsilonArcLabelsFrom(N node)
    {
        return arcLabelsFrom(node).toSet().toImmutable().newWithout(epsilonLabel());
    }

    default N directSuccessorOf(N node, A arcLabel)
    {
        if (!arcDeterministic() || arcLabel.equals(epsilonLabel())) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        return directSuccessorsOf(node, arcLabel).getOnly();
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

        MutableSet<N> base = UnifiedSet.newSet(referredNodes().size()); // upper bound
        base.addAllIterable(nodes);
        while (true) {
            if (!base.addAllIterable(directSuccessorsOf(base, epsilonLabel()))) {
                break; // `base` has converged
            }
        }

        return arcLabel.equals(epsilonLabel()) ? base : epsilonClosureOf(directSuccessorsOf(base, arcLabel));
    }

    @Override
    String toString();

    String toStringWith(ImmutableMap<N, String> nodeNameOverride);

    interface Builder<N, A>
    {
        int currentSize();

        boolean isEmpty();

        Builder<N, A> addArc(N from, N to, A arcLabel);

        Builder<N, A> removeArc(N from, N to, A arcLabel);

        Builder<N, A> removeNode(N node);

        Digraph<N, A> asGraph();

        TransitionGraph<N, A> build();
    }

    interface Provider
    {
        <N, A> Builder<N, A> builder(int nodeCapacity, int arcCapacity, A epsilonLabel);

        <N, A> Builder<N, A> builder(TransitionGraph<N, A> base);
    }
}
