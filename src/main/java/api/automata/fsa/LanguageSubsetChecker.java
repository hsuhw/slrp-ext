package api.automata.fsa;

import api.automata.State;
import org.eclipse.collections.api.list.ListIterable;

public interface LanguageSubsetChecker<T>
{
    Result<T> test(FSA<State<T>, T> subsumer, FSA<State<T>, T> includer);

    interface Result<T>
    {
        boolean passed();

        default boolean rejected()
        {
            return !passed();
        }

        Counterexample<T> counterexample();

        @Override
        String toString();
    }

    interface Counterexample<T>
    {
        FSA<State<T>, T> sourceImage();

        default ListIterable<T> get()
        {
            return sourceImage().enumerateOneShortest();
        }

        @Override
        String toString();
    }
}
