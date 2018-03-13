package api.automata.fsa;

import org.eclipse.collections.api.list.ListIterable;

public interface LanguageSubsetChecker
{
    <S> Result<S> test(FSA<S> subsumer, FSA<S> includer);

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

        default ListIterable<S> witness()
        {
            return sourceImage().enumerateOneShortest();
        }

        @Override
        String toString();
    }
}
