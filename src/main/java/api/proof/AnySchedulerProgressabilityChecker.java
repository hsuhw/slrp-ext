package api.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Twin;

public interface AnySchedulerProgressabilityChecker
{
    <S> Result<S> test(FSA<Twin<S>> scheduler, FSA<Twin<S>> process, FSA<S> invariant, FSA<Twin<S>> order);

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
        ImmutableList<Twin<S>> get();

        @Override
        String toString();
    }
}
