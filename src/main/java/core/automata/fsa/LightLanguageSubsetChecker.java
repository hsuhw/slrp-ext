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
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class LightLanguageSubsetChecker<S> implements LanguageSubsetChecker<S>
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
            return new Result(false, new Counterexample(subsumer.enumerateOneShortest()));
        }
        final FSA<S> includerFixed = includer.determinize().complete();
        if (includerFixed.complement().acceptsNone()) { // universe includes anyone
            return new Result(true, null);
        }

        final ListIterable<S> divergentWitness = new DivergentWitnessBFS(subsumer, includerFixed).run();

        return divergentWitness == null
               ? new Result(true, null)
               : new Result(false, new Counterexample(divergentWitness));
    }

    private class DivergentWitnessBFS
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

        private void visit(Twin<State<S>> target, Twin<State<S>> fromPair, S withSymbol)
        {
            visitRecord.computeIfAbsent(target, pair -> {
                pendingChecks.add(pair);
                return Tuples.pair(fromPair, withSymbol);
            });
        }

        private void handleEpsilonTransitions(Twin<State<S>> statePair)
        {
            final State<S> dept1 = statePair.getOne();
            final State<S> dept2 = statePair.getOne();
            dept1.successors(epsilon).forEach(dest -> visit(Tuples.twin(dest, dept2), statePair, epsilon));
            if (dept2.enabledSymbols().contains(epsilon)) {
                throw new IllegalStateException("includer should be deterministic");
            }
        }

        private ListIterable<S> witnessFoundAt(Twin<State<S>> statePair, S breakingStep)
        {
            final MutableList<S> witnessBacktrace = Lists.mutable.empty();

            witnessBacktrace.add(breakingStep);
            Twin<State<S>> currStatePair = statePair;
            while (!currStatePair.equals(startStatePair)) {
                final Pair<Twin<State<S>>, S> visitorAndSymbol = visitRecord.get(currStatePair);
                witnessBacktrace.add(visitorAndSymbol.getTwo());
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
            boolean includerAcceptsHere;
            while ((currStatePair = pendingChecks.poll()) != null) {
                handleEpsilonTransitions(currStatePair);
                final State<S> dept1 = currStatePair.getOne();
                final State<S> dept2 = currStatePair.getOne();
                for (S symbol : dept1.enabledSymbols()) {
                    if (symbol.equals(epsilon)) {
                        continue; // already handled
                    }
                    final State<S> dest2 = dept2.successor(symbol); // includer should be deterministic
                    includerAcceptsHere = includer.isAcceptState(dest2);
                    for (State<S> dest1 : dept1.successors(symbol)) {
                        if (subsumer.isAcceptState(dest1) && !includerAcceptsHere) {
                            return witnessFoundAt(currStatePair, symbol);
                        }
                        visit(Tuples.twin(dest1, dest2), currStatePair, symbol);
                    }
                }
            }

            return null;
        }
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
        public ListIterable<S> get()
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
