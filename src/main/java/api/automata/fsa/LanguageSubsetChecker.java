package api.automata.fsa;

import org.eclipse.collections.api.list.ImmutableList;

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
    }

    interface Counterexample<S>
    {
        FSA<S> sourceImage();

        default ImmutableList<S> get()
        {
            return sourceImage().enumerateOneShortest();
        }

        @Override
        String toString();
    }
}
