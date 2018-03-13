package api.proof;

import api.automata.fsa.FSA;
import api.automata.fst.FST;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;

public interface BehaviorEnclosureChecker
{
    <S> Result<S> test(FST<S, S> behavior, FSA<S> encloser);

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
        RichIterable<ListIterable<S>> causes();

        ListIterable<S> invalidStep();

        @Override
        String toString();
    }
}
