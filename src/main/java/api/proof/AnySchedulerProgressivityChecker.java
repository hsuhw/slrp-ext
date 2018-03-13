package api.proof;

import api.automata.fsa.FSA;
import api.automata.fst.FST;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Twin;

public interface AnySchedulerProgressivityChecker
{
    <S> Result<S> test(FST<S, S> scheduler, FST<S, S> process, FSA<S> invariant, FST<S, S> order);

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
        ListIterable<Twin<S>> fruitlessStep();

        @Override
        String toString();
    }
}
