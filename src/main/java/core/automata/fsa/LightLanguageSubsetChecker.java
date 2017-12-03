package core.automata.fsa;

import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.AutomatonManipulator.selectFrom;
import static api.util.Connectives.AND;
import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public class LightLanguageSubsetChecker implements LanguageSubsetChecker
{
    @Override
    public <S> Result<S> test(FSA<S> subsumer, FSA<S> includer)
    {
        if (subsumer.acceptsNone()) {
            return new Result<>(true, null); // anyone includes empty
        }
        final FSA<S> incFixed = FSAs.complete(FSAs.determinize(includer));
        if (incFixed.acceptsNone()) {
            final ImmutableList<S> witness = subsumer.enumerateOneShortest();
            return new Result<>(false, new Counterexample<>(witness)); // empty includes nobody
        }

        final boolean[] witnessFound = {false};
        final MutableSet<Twin<State>> witnessStatePair = UnifiedSet.newSet(1);
        final FSA<S> observingImage = FSAs.product(subsumer, incFixed, subsumer.alphabet(), (pair, s1, s2) -> {
            final State state1 = pair.getOne();
            final State state2 = pair.getTwo();
            if (!witnessFound[0] && subsumer.isAcceptState(state1) && !incFixed.isAcceptState(state2)) {
                witnessFound[0] = true; // short-circuit the production if a witness is found
                witnessStatePair.add(pair);
            }
            return witnessFound[0] ? null : (s1.equals(s2) ? s1 : null);
        }, (stateMapping, builder) -> {
            builder.addStartStates(selectFrom(stateMapping, subsumer::isStartState, AND, incFixed::isStartState));
            if (witnessStatePair.notEmpty()) {
                builder.addAcceptState(stateMapping.get(witnessStatePair.getOnly()));
            }
        });

        return observingImage.acceptsNone()
               ? new Result<>(true, null)
               : new Result<>(false, new Counterexample<>(observingImage.enumerateOneShortest()));
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
        private ImmutableList<S> instance;

        Counterexample(ImmutableList<S> instance)
        {
            this.instance = instance;
        }

        @Override
        public FSA<S> sourceImage()
        {
            throw new UnsupportedOperationException("image not available on light instances");
        }

        @Override
        public ImmutableList<S> get()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of nonincluded parts: " + get();
        }
    }
}
