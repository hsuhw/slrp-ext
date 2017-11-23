package core.proof;

import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.FairnessProgressabilityChecker;
import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;

public class BasicFairnessProgressabilityChecker implements FairnessProgressabilityChecker
{
    @Override
    public <S> Result<S> test(FSA<Twin<S>> behavior, FSA<S> nonfinalConfigs, FSA<S> invariant, FSA<Twin<S>> order)
    {
        final FSA<Twin<S>> smallerStepMoves = FSAs.intersect(behavior, order);
        final FSA<S> smallerStepAvail = FSAs.project(smallerStepMoves, invariant.alphabet(), Twin::getOne);
        final FSA<S> nonfinalInv = FSAs.intersect(invariant, nonfinalConfigs);
        final LanguageSubsetChecker.Result<S> nonfinalSmallerAvail = FSAs.checkSubset(nonfinalInv, smallerStepAvail);

        if (nonfinalSmallerAvail.passed()) {
            return new Result<>(true, null);
        }
        final FSA<S> witnessImage = nonfinalSmallerAvail.counterexample().sourceImage();

        return new Result<>(false, new Counterexample<>(behavior, witnessImage));
    }

    private class Result<S> implements FairnessProgressabilityChecker.Result<S>
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
            return passed() ? "pass" : counterexample().toString();
        }
    }

    private class Counterexample<S> implements FairnessProgressabilityChecker.Counterexample<S>
    {
        private final FSA<Twin<S>> behavior;
        private final FSA<S> sourceImage;
        private ImmutableSet<ImmutableList<S>> causes;
        private ImmutableList<S> instance;

        Counterexample(FSA<Twin<S>> behavior, FSA<S> source)
        {
            this.behavior = behavior;
            sourceImage = source;
        }

        @Override
        public FSA<S> sourceImage()
        {
            return sourceImage;
        }

        @Override
        public ImmutableSet<ImmutableList<S>> causes()
        {
            if (causes == null) {
                causes = Transducers.postImage(behavior, get());
            }

            return causes;
        }

        @Override
        public ImmutableList<S> get()
        {
            if (instance == null) {
                instance = FairnessProgressabilityChecker.Counterexample.super.get();
                Assertions.referenceNotNull(instance); // a counterexample will always have a witness
            }

            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of non-progressable parts: " + get() + " causes: " + causes().makeString();
        }
    }
}
