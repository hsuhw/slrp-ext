package api.automata.fsa;

import api.automata.Alphabet;
import org.eclipse.collections.api.list.ListIterable;

public interface LStarLearning
{
    <S> FSA<S> learn(Alphabet<S> alphabet, Teacher<S> teacher);

    interface Teacher<S>
    {
        boolean targetAccepts(ListIterable<S> word);

        EquivalenceCheckResult<S> checkAnswer(FSA<S> answer);
    }

    interface EquivalenceCheckResult<S>
    {
        boolean passed();

        ListIterable<S> positiveCounterexample();

        ListIterable<S> negativeCounterexample();
    }
}
