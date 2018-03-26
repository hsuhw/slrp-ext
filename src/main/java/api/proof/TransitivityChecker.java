package api.proof;

import api.automata.fst.FST;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;

public interface TransitivityChecker
{
    <S> Result<S> test(FST<S, S> target);

    interface Result<S>
    {
        boolean passed();

        default boolean rejected()
        {
            return !passed();
        }

        Counterexample<S> counterexample();

        @Override
        String toString();
    }

    interface Counterexample<S>
    {
        RichIterable<ListIterable<S>> validMiddleSteps();

        ListIterable<Pair<S, S>> breakingStep();

        @Override
        String toString();
    }
}
