package core.proof;

import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fst.FST;
import api.proof.FairnessProgressivityChecker;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class BasicFairnessProgressivityChecker implements FairnessProgressivityChecker
{
    @Override
    public <S> Result<S> test(FST<S, S> behavior, FSA<S> matteringConfigs, FSA<S> invariant, FST<S, S> order)
    {
        final FST<S, S> smallerMoves = behavior.intersect(order);
        final FSA<S> smallerAvails = smallerMoves.domain();
        final FSA<S> matteringInvariant = matteringConfigs.intersect(invariant);
        final LanguageSubsetChecker.Result<S> someSmallerAvail = smallerAvails.checkContaining(matteringInvariant);

        if (someSmallerAvail.passed()) {
            return new Result<>(true, null);
        }

        return new Result<>(false, new Counterexample<>(behavior, someSmallerAvail.counterexample().witness()));
    }

    public static class Result<S> implements FairnessProgressivityChecker.Result<S>
    {
        private final boolean passed;
        private final FairnessProgressivityChecker.Counterexample<S> counterexample;

        public Result(boolean passed, FairnessProgressivityChecker.Counterexample<S> counterexample)
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
        public FairnessProgressivityChecker.Counterexample<S> counterexample()
        {
            return counterexample;
        }

        @Override
        public String toString()
        {
            return passed() ? "pass" : DISPLAY_NEWLINE + DISPLAY_INDENT + "-- " + counterexample();
        }
    }

    public static class Counterexample<S> implements FairnessProgressivityChecker.Counterexample<S>
    {
        private ListIterable<S> instance;
        private final FST<S, S> behavior;
        private RichIterable<ListIterable<S>> possibleProgressCandidates;

        public Counterexample(FST<S, S> behavior, ListIterable<S> instance)
        {
            this.behavior = behavior;
            this.instance = instance;
        }

        @Override
        public RichIterable<ListIterable<S>> possibleProgressSteps()
        {
            if (possibleProgressCandidates == null) {
                possibleProgressCandidates = behavior.preImage(fruitlessStep());
            }

            return possibleProgressCandidates;
        }

        @Override
        public ListIterable<S> fruitlessStep()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of non-progressive parts: " + fruitlessStep() + " possible progress steps: " +
                possibleProgressSteps().makeString();
        }
    }
}
