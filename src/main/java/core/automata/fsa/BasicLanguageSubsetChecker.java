package core.automata.fsa;

import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import common.util.Assert;
import org.eclipse.collections.api.list.ListIterable;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class BasicLanguageSubsetChecker<S> implements LanguageSubsetChecker<S>
{
    @Override
    public Result test(FSA<S> subsumer, FSA<S> includer)
    {
        if (!subsumer.alphabet().epsilon().equals(includer.alphabet().epsilon())) {
            throw new IllegalArgumentException("incompatible two alphabet given");
        }

        if (subsumer.acceptsNone()) { // anyone includes empty
            return new Result(true, null);
        }
        if (includer.acceptsNone()) { // empty includes nobody
            return new Result(false, new Counterexample(subsumer));
        }
        final FSA<S> includerBar = includer.complement();
        if (includerBar.acceptsNone()) { // universe includes anyone
            return new Result(true, null);
        }

        final FSA<S> divergentImage = includerBar.intersect(subsumer);

        return divergentImage.acceptsNone()
               ? new Result(true, null)
               : new Result(false, new Counterexample(divergentImage));
    }

    private class Result implements LanguageSubsetChecker.Result<S>
    {
        private final boolean passed;
        private final Counterexample counterexample;

        private Result(boolean passed, Counterexample counterexample)
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
        public Counterexample counterexample()
        {
            return counterexample;
        }

        @Override
        public String toString()
        {
            return passed() ? "pass" : DISPLAY_NEWLINE + DISPLAY_INDENT + "-- " + counterexample();
        }
    }

    private class Counterexample implements LanguageSubsetChecker.Counterexample<S>
    {
        private final FSA<S> sourceImage;
        private ListIterable<S> instance;

        private Counterexample(FSA<S> source)
        {
            sourceImage = source;
        }

        @Override
        public FSA<S> sourceImage()
        {
            return sourceImage;
        }

        @Override
        public ListIterable<S> get()
        {
            if (instance == null) {
                instance = LanguageSubsetChecker.Counterexample.super.get();
                Assert.referenceNotNull(instance); // a counterexample will always have a witness
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
