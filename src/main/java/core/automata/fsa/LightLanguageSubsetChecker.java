package core.automata.fsa;

import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class LightLanguageSubsetChecker implements LanguageSubsetChecker
{
    @Override
    public <S> Result<S> test(FSA<S> subsumer, FSA<S> includer)
    {
        if (!subsumer.alphabet().epsilon().equals(includer.alphabet().epsilon())) {
            throw new IllegalArgumentException("incompatible two alphabet given");
        }

        if (subsumer.acceptsNone()) { // anyone includes empty
            return new Result<>(true, null);
        }
        if (includer.acceptsNone()) { // empty includes nobody
            return new Result<>(false, new Counterexample<>(subsumer.enumerateOneShortest()));
        }
        final FSA<S> includerFixed = includer.determinize().complete();
        if (includerFixed.complement().acceptsNone()) { // universe includes anyone
            return new Result<>(true, null);
        }

        final ListIterable<S> divergentWitness = new DivergentWitnessBFS<>(subsumer, includerFixed.minimize()).run();

        return divergentWitness == null
               ? new Result<>(true, null)
               : new Result<>(false, new Counterexample<>(divergentWitness));
    }

    private class DivergentWitnessBFS<S>
    {
        private final S epsilon;
        private final FSA<S> subsumer;
        private final FSA<S> includer;
        private final Twin<State<S>> startStatePair;
        private final MutableMap<Twin<State<S>>, Pair<Twin<State<S>>, S>> visitRecord;
        private final Queue<Twin<State<S>>> pendingChecks;

        private DivergentWitnessBFS(FSA<S> subsumer, FSA<S> includer)
        {
            epsilon = subsumer.alphabet().epsilon();
            this.subsumer = subsumer;
            this.includer = includer;
            startStatePair = Tuples.twin(subsumer.startState(), includer.startState());
            visitRecord = UnifiedMap.newMap(subsumer.states().size() * includer.states().size()); // upper bound
            pendingChecks = new LinkedList<>();
        }

        private void visit(Twin<State<S>> target, Twin<State<S>> from, S withSymbol)
        {
            visitRecord.computeIfAbsent(target, pair -> {
                pendingChecks.add(pair);
                return Tuples.pair(from, withSymbol);
            });
        }

        private ListIterable<S> witnessFoundAt(Twin<State<S>> statePair, S breakingStep)
        {
            final MutableList<S> witnessBacktrace = FastList.newList();

            if (breakingStep != epsilon) {
                witnessBacktrace.add(breakingStep);
            }
            Twin<State<S>> currStatePair = statePair;
            S currSymbol;
            while (!currStatePair.equals(startStatePair)) {
                final Pair<Twin<State<S>>, S> visitorAndSymbol = visitRecord.get(currStatePair);
                if (!(currSymbol = visitorAndSymbol.getTwo()).equals(epsilon)) {
                    witnessBacktrace.add(currSymbol);
                }
                currStatePair = visitorAndSymbol.getOne();
            }

            return witnessBacktrace.reverseThis();
        }

        private ListIterable<S> run()
        {
            if (subsumer.isAcceptState(subsumer.startState()) && !includer.isAcceptState(includer.startState())) {
                return Lists.immutable.empty();
            }

            visit(startStatePair, null, null);
            Twin<State<S>> currStatePair;
            State<S> dept1, dept2, dest2;
            boolean includerAccepts;
            while ((currStatePair = pendingChecks.poll()) != null) {
                dept1 = currStatePair.getOne();
                dept2 = currStatePair.getTwo();
                if (dept2.enabledSymbols().contains(epsilon)) {
                    throw new IllegalStateException("includer should be deterministic");
                }
                for (S symbol : dept1.enabledSymbols()) {
                    dest2 = symbol.equals(epsilon) ? dept2 : dept2.successor(symbol);
                    includerAccepts = includer.isAcceptState(dest2);
                    for (State<S> dest1 : dept1.successors(symbol)) {
                        if (subsumer.isAcceptState(dest1) && !includerAccepts) {
                            return witnessFoundAt(currStatePair, symbol);
                        }
                        visit(Tuples.twin(dest1, dest2), currStatePair, symbol);
                    }
                }
            }

            return null;
        }
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
        private ListIterable<S> instance;

        private Counterexample(ListIterable<S> instance)
        {
            this.instance = instance;
        }

        @Override
        public FSA<S> sourceImage()
        {
            throw new UnsupportedOperationException("image not available on light instances");
        }

        @Override
        public ListIterable<S> witness()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of nonincluded parts: " + witness();
        }
    }
}
