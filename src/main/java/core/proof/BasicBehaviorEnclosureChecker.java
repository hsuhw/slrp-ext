package core.proof;

import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fst.FST;
import api.proof.BehaviorEnclosureChecker;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class BasicBehaviorEnclosureChecker implements BehaviorEnclosureChecker
{
    @Override
    public <S> Result<S> test(FST<S, S> behavior, FSA<S> encloser)
    {
        final FSA<S> postBehavior = behavior.postImage(encloser);
        final LanguageSubsetChecker.Result<S> enclosureCheck = encloser.checkContaining(postBehavior);

        if (enclosureCheck.passed()) {
            return new Result<>(true, null);
        }

        return new Result<>(false, new Counterexample<>(behavior, enclosureCheck.counterexample().witness()));
    }

    private class Result<S> implements BehaviorEnclosureChecker.Result<S>
    {
        private final boolean passed;
        private final Counterexample<S> counterexample;

        private Result(boolean passed, Counterexample<S> counterexample)
        {
            this.passed = passed;
            this.counterexample = counterexample;
        }

        @Override
        public boolean passed()
        {
            return passed;
        }

        @Override
        public Counterexample<S> counterexample()
        {
            return counterexample;
        }

        @Override
        public String toString()
        {
            return passed() ? "pass" : DISPLAY_NEWLINE + DISPLAY_INDENT + "-- " + counterexample();
        }
    }

    private class Counterexample<S> implements BehaviorEnclosureChecker.Counterexample<S>
    {
        private final ListIterable<S> invalidStep;
        private final FST<S, S> behavior;
        private RichIterable<ListIterable<S>> causes;

        private Counterexample(FST<S, S> behavior, ListIterable<S> instance)
        {
            this.behavior = behavior;
            this.invalidStep = instance;
        }

        @Override
        public RichIterable<ListIterable<S>> causes()
        {
            if (causes == null) {
                causes = behavior.preImage(invalidStep);
            }

            return causes;
        }

        @Override
        public ListIterable<S> invalidStep()
        {
            return invalidStep;
        }

        @Override
        public String toString()
        {
            return "witness of nonenclosed parts: " + invalidStep() + " causes: " + causes().makeString();
        }
    }
}
