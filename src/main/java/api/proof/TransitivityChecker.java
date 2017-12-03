package api.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;

public interface TransitivityChecker
{
    <S> Result<S> test(FSA<Twin<S>> target);

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
        ImmutableSet<Twin<ImmutableList<Twin<S>>>> causes();

        ImmutableList<Twin<S>> get();

        @Override
        String toString();
    }
}
