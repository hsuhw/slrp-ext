package core.automata.fsa;

import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public class BasicLanguageSubsetChecker implements LanguageSubsetChecker
{
    @Override
    public <S> Result<S> test(FSA<S> subsumer, FSA<S> includer)
    {
        if (subsumer.acceptsNone()) {
            return new Result<>(true, null); // anyone includes empty
        }
        if (includer.acceptsNone()) {
            return new Result<>(false, new Counterexample<>(subsumer)); // empty includes nobody
        }
        final FSA<S> includerBar = FSAs.complement(includer);
        if (includerBar.acceptsNone()) {
            return new Result<>(true, null); // universe includes anyone
        }

        final FSA<S> observingImage = FSAs.intersect(includerBar, subsumer);

        return observingImage.acceptsNone()
               ? new Result<>(true, null)
               : new Result<>(false, new Counterexample<>(observingImage));
    }

    private class Result<S> implements LanguageSubsetChecker.Result<S>
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

    private class Counterexample<S> implements LanguageSubsetChecker.Counterexample<S>
    {
        private final FSA<S> sourceImage;
        private ImmutableList<S> instance;

        Counterexample(FSA<S> source)
        {
            sourceImage = source;
        }

        @Override
        public FSA<S> sourceImage()
        {
            return sourceImage;
        }

        @Override
        public ImmutableList<S> get()
        {
            if (instance == null) {
                instance = LanguageSubsetChecker.Counterexample.super.get();
                Assertions.referenceNotNull(instance); // a counterexample will always have a witness
            }

            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of nonincluded parts: " + get();
        }
    }
}
