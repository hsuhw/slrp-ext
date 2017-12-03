package api.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;

public interface BehaviorEnclosureChecker
{
    <S> Result<S> test(FSA<Twin<S>> behavior, FSA<S> encloser);

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
        ImmutableSet<ImmutableList<S>> causes();

        ImmutableList<S> get();

        @Override
        String toString();
    }
}
