package core.proof;

import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.AnySchedulerProgressabilityChecker;
import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.AutomatonManipulator.selectFrom;
import static api.automata.fsa.FSAs.checkSubset;
import static api.util.Connectives.AND;
import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public class BasicAnySchedulerProgressabilityChecker implements AnySchedulerProgressabilityChecker
{
    @Override
    public <S> Result<S> test(FSA<Twin<S>> nfSched, FSA<Twin<S>> proc, FSA<S> nfConfigs, FSA<S> inv, FSA<Twin<S>> order)
    {
        final FSA<S> nfInv = FSAs.intersect(inv, nfConfigs);
        final FSA<Twin<S>> anySchedMove = Transducers.filterByInput(nfSched, nfInv);
        final FSA<Twin<S>> smallerStepAvail = FSAs.product(proc, order, proc.alphabet(), (trans, ord) -> {
            return trans.getTwo().equals(ord.getTwo()) ? Tuples.twin(ord.getOne(), trans.getOne()) : null;
        }, (stateMapping, builder) -> {
            builder.addStartStates(selectFrom(stateMapping, proc::isStartState, AND, order::isStartState));
            builder.addAcceptStates(selectFrom(stateMapping, proc::isAcceptState, AND, order::isAcceptState));
        });
        final LanguageSubsetChecker.Result<Twin<S>> alwaysSmallerAvail = checkSubset(anySchedMove, smallerStepAvail);

        if (alwaysSmallerAvail.passed()) {
            return new Result<>(true, null);
        }
        final FSA<Twin<S>> witnessImage = alwaysSmallerAvail.counterexample().sourceImage();

        return new Result<>(false, new Counterexample<>(witnessImage));
    }

    private class Result<S> implements AnySchedulerProgressabilityChecker.Result<S>
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

    private class Counterexample<S> implements AnySchedulerProgressabilityChecker.Counterexample<S>
    {
        private final FSA<Twin<S>> sourceImage;
        private ImmutableList<Twin<S>> instance;

        Counterexample(FSA<Twin<S>> source)
        {
            sourceImage = source;
        }

        @Override
        public FSA<Twin<S>> sourceImage()
        {
            return sourceImage;
        }

        @Override
        public ImmutableList<Twin<S>> get()
        {
            if (instance == null) {
                instance = AnySchedulerProgressabilityChecker.Counterexample.super.get();
                Assertions.referenceNotNull(instance); // a counterexample will always have a witness
            }

            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of non-progressable parts: " + get();
        }
    }
}
