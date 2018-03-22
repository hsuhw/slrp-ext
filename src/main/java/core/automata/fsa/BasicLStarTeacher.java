package core.automata.fsa;


import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ListIterable;

import static api.automata.fsa.LStarLearning.EquivalenceCheckResult;
import static api.automata.fsa.LStarLearning.Teacher;

public class BasicLStarTeacher<S> implements Teacher<S>
{
    private final FSA<S> target;

    public BasicLStarTeacher(FSA<S> target)
    {
        this.target = target;
    }

    @Override
    public boolean targetAccepts(ListIterable<S> word)
    {
        return target.accepts(word);
    }

    @Override
    public EquivalenceCheckResult<S> checkAnswer(FSA<S> answer)
    {
        final var targetContainsAnswer = target.checkContaining(answer);
        final var answerContainsTarget = answer.checkContaining(target);

        return new EquivalenceCheckResult<>()
        {
            @Override
            public boolean passed()
            {
                return targetContainsAnswer.passed() && answerContainsTarget.passed();
            }

            @Override
            public ListIterable<S> positiveCounterexample()
            {
                return answerContainsTarget.passed() ? null : answerContainsTarget.counterexample().witness();
            }

            @Override
            public ListIterable<S> negativeCounterexample()
            {
                return targetContainsAnswer.passed() ? null : targetContainsAnswer.counterexample().witness();
            }
        };
    }
}
