package api.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;

public interface FairnessProgressabilityChecker
{
    <S> Result<S> test(FSA<Twin<S>> behavior, FSA<S> nonfinalConfigs, FSA<S> invariant, FSA<Twin<S>> order);

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
        FSA<S> sourceImage();

        ImmutableSet<ImmutableList<S>> causes();

        default ImmutableList<S> get()
        {
            return sourceImage().enumerateOneShortest();
        }

        @Override
        String toString();
    }
}
