package core.proof;

import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.BehaviorEnclosureChecker;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public class BasicBehaviorEnclosureChecker implements BehaviorEnclosureChecker
{
    @Override
    public <S> Result<S> test(FSA<Twin<S>> behavior, FSA<S> encloser)
    {
        final FSA<S> postBehavior = Transducers.postImage(behavior, encloser);
        final LanguageSubsetChecker.Result<S> postBehaviorEnclosed = FSAs.checkSubset(postBehavior, encloser);

        if (postBehaviorEnclosed.passed()) {
            return new Result<>(true, null);
        }

        return new Result<>(false, new Counterexample<>(behavior, postBehaviorEnclosed.counterexample().get()));
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
        private ImmutableList<S> instance;
        private final FSA<Twin<S>> behavior;
        private ImmutableSet<ImmutableList<S>> causes;

        Counterexample(FSA<Twin<S>> behavior, ImmutableList<S> instance)
        {
            this.behavior = behavior;
            this.instance = instance;
        }

        @Override
        public ImmutableSet<ImmutableList<S>> causes()
        {
            if (causes == null) {
                causes = Transducers.preImage(behavior, get());
            }

            return causes;
        }

        @Override
        public ImmutableList<S> get()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of nonenclosed parts: " + get() + " causes: " + causes().makeString();
        }
    }
}
