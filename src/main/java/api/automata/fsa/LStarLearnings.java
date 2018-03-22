package api.automata.fsa;

import core.automata.fsa.BasicLStarLearning;
import core.automata.fsa.BasicLStarTeacher;

import static api.automata.fsa.LStarLearning.Teacher;

public final class LStarLearnings
{
    private LStarLearnings()
    {
    }

    public static LStarLearning learner()
    {
        return new BasicLStarLearning();
    }

    public static <S> Teacher<S> teacher(FSA<S> target)
    {
        return new BasicLStarTeacher<>(target);
    }
}
