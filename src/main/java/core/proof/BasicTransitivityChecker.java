package core.proof;

import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.TransitivityChecker;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Set;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public class BasicTransitivityChecker implements TransitivityChecker
{
    @Override
    public <S> Result<S> test(FSA<Twin<S>> target)
    {
        final FSA<Twin<S>> transitive = Transducers.compose(target, target, target.alphabet());

        final LanguageSubsetChecker.Result<Twin<S>> targetBeTransitive = FSAs.checkSubset(transitive, target);
        if (targetBeTransitive.passed()) {
            return new Result<>(true, null);
        }

        return new Result<>(false, new Counterexample<>(target, targetBeTransitive.counterexample().get()));
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
        private ImmutableList<Twin<S>> instance;
        private final FSA<Twin<S>> relation;
        private ImmutableSet<Twin<ImmutableList<Twin<S>>>> causes;

        private Counterexample(FSA<Twin<S>> relation, ImmutableList<Twin<S>> instance)
        {
            this.instance = instance;
            this.relation = relation;
        }

        @Override
        public ImmutableSet<Twin<ImmutableList<Twin<S>>>> causes()
        {
            if (causes == null) {
                // 'x -> z' is the witness; 'x -> { y1, y2, ... } -> z' are the causes
                final ImmutableList<Twin<S>> witness = get();
                final ImmutableList<S> x = witness.collect(Twin::getOne);
                final ImmutableList<S> z = witness.collect(Twin::getTwo);
                final Set<ImmutableList<S>> xPostImage = Transducers.postImage(relation, x).castToSet();
                final Set<ImmutableList<S>> zPreImage = Transducers.preImage(relation, z).castToSet();
                final ImmutableSet<ImmutableList<S>> ys = Sets.intersect(xPostImage, zPreImage).toImmutable();
                causes = ys.collect(y -> Tuples.twin(Alphabets.twinWord(x, y), Alphabets.twinWord(y, z)));
            }

            return causes;
        }

        @Override
        public ImmutableList<Twin<S>> get()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of intransitive parts: " + get() + " causes: " + causes().makeString();
        }
    }
}
