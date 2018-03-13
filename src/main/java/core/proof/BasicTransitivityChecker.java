package core.proof;

import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fst.FST;
import api.proof.TransitivityChecker;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class BasicTransitivityChecker implements TransitivityChecker
{
    @Override
    public <S> Result<S> test(FST<S, S> target)
    {
        final FST<S, S> transitive = target.compose(target, target.alphabet());
        final LanguageSubsetChecker.Result<Pair<S, S>> transitivityCheck = target.asFSA()
                                                                                 .checkContaining(transitive.asFSA());
        if (transitivityCheck.passed()) {
            return new Result<>(true, null);
        }

        return new Result<>(false, new Counterexample<>(target, transitivityCheck.counterexample().witness()));
    }

    private class Result<S> implements TransitivityChecker.Result<S>
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

    private class Counterexample<S> implements TransitivityChecker.Counterexample<S>
    {
        private final ListIterable<Pair<S, S>> invalidStep;
        private final FST<S, S> relation;
        private RichIterable<ListIterable<S>> validMiddleSteps;

        private Counterexample(FST<S, S> relation, ListIterable<Pair<S, S>> witness)
        {
            this.invalidStep = witness;
            this.relation = relation;
        }

        @Override
        public RichIterable<ListIterable<S>> validMiddleSteps()
        {
            if (validMiddleSteps == null) {
                // 'x -> z' is the invalidStep; for 'x -> { y1, y2, ... } -> z', ys are the valid middle steps
                final ListIterable<S> x = invalidStep.collect(Pair::getOne);
                final ListIterable<S> z = invalidStep.collect(Pair::getTwo);
                validMiddleSteps = relation.postImage(x).select(relation.preImage(z)::contains);
            }

            return validMiddleSteps;
        }

        @Override
        public ListIterable<Pair<S, S>> invalidStep()
        {
            return invalidStep;
        }

        @Override
        public String toString()
        {
            return "witness of intransitive parts: " + invalidStep() + " valid middle steps: " +
                validMiddleSteps().makeString();
        }
    }
}
