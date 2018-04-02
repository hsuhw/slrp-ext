package api.automata.fsa;

import api.automata.Alphabet;
import common.util.InterruptException;
import org.eclipse.collections.api.list.ListIterable;

public interface LStarLearning
{
    <S> FSA<S> learn(Alphabet<S> alphabet, Teacher<S> teacher) throws InterruptException;

    interface Teacher<S>
    {
        boolean targetAccepts(ListIterable<S> word);

        EquivalenceCheckResult<S> checkAnswer(FSA<S> answer) throws InterruptException;
    }

    interface EquivalenceCheckResult<S>
    {
        boolean passed();

        default boolean rejected()
        {
            return !passed();
        }

        ListIterable<S> positiveCounterexample();

        ListIterable<S> negativeCounterexample();
    }
}
