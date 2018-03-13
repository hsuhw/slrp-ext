package core.proof;

import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fst.FST;
import api.proof.AnySchedulerProgressivityChecker;
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

import static api.automata.Automaton.StepMaker;
import static api.util.Connectives.AND;
import static api.util.Connectives.AcceptStates;
import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.DISPLAY_NEWLINE;

public class BasicAnySchedulerProgressivityChecker implements AnySchedulerProgressivityChecker
{
    @Override
    public <S> Result<S> test(FST<S, S> nonfinalScheduler, FST<S, S> process, FSA<S> invariant, FST<S, S> order)
    {
        final FST<S, S> processInvariantMoves = process.maskByOutput(invariant);

        final StepMaker<Pair<S, S>, Pair<S, S>, Pair<S, S>> smallerAvailSchedulerPair = //
            (statePair, processMove, greaterThan) -> {
                final boolean processMovesToSmaller = processMove.getTwo().equals(greaterThan.getTwo());
                final S schedulerDept = greaterThan.getOne();
                final S schedulerDest = processMove.getOne();
                return processMovesToSmaller ? Tuples.pair(schedulerDept, schedulerDest) : null;
            };

        final FST<S, S> smallerAvailSchedulerMoves = //
            (FST<S, S>) processInvariantMoves.product(order, process.alphabet(), smallerAvailSchedulerPair,
                                                      AcceptStates.select(processInvariantMoves, order, AND));

        final ListIterable<Twin<S>> witness = new CounterexampleBFS<>(nonfinalScheduler.asFSA(), invariant,
                                                                      smallerAvailSchedulerMoves.asFSA()).run();

        return witness == null //
               ? new Result<>(true, null) //
               : new Result<>(false, new Counterexample<>(witness));
    }

    public static class Result<S> implements AnySchedulerProgressivityChecker.Result<S>
    {
        private final boolean passed;
        private final AnySchedulerProgressivityChecker.Counterexample<S> counterexample;

        public Result(boolean passed, AnySchedulerProgressivityChecker.Counterexample<S> counterexample)
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
        public AnySchedulerProgressivityChecker.Counterexample<S> counterexample()
        {
            return counterexample;
        }

        @Override
        public String toString()
        {
            return passed() ? "pass" : DISPLAY_NEWLINE + DISPLAY_INDENT + "-- " + counterexample();
        }
    }

    public static class Counterexample<S> implements AnySchedulerProgressivityChecker.Counterexample<S>
    {
        private ListIterable<Twin<S>> instance;

        public Counterexample(ListIterable<Twin<S>> instance)
        {
            this.instance = instance;
        }

        @Override
        public ListIterable<Twin<S>> fruitlessStep()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of non-progressive parts: " + fruitlessStep();
        }
    }

    private class StateTuple<S>
    {
        private final State<Pair<S, S>> sched;
        private final State<S> inv;
        private final State<Pair<S, S>> rhs;
        private int hashCode = -1;

        private StateTuple(State<Pair<S, S>> scheduler, State<S> invariant, State<Pair<S, S>> rightHandSide)
        {
            sched = scheduler;
            inv = invariant;
            rhs = rightHandSide;
        }

        @Override
        public int hashCode()
        {
            if (hashCode == -1) {
                final int prime = 67;
                int result = 1;

                result = prime * result + sched.hashCode();
                result = prime * result + inv.hashCode();
                result = prime * result + rhs.hashCode();

                hashCode = result;
            }

            return hashCode;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) {
                return true;
            }

            if (obj instanceof StateTuple<?>) {
                try {
                    @SuppressWarnings("unchecked")
                    final StateTuple<S> other = (StateTuple<S>) obj;
                    return other.sched.equals(this.sched) && other.inv.equals(this.inv) && other.rhs.equals(this.rhs);
                } catch (ClassCastException e) {
                    return false;
                }
            }

            return false;
        }
    }

    private class CounterexampleBFS<S>
    {
        private final S epsilon;
        private final Pair<S, S> epsilonPair;
        private final FSA<Pair<S, S>> nonfinalScheduler;
        private final FSA<S> invariant;
        private final FSA<Pair<S, S>> rhs;
        private final StateTuple<S> startStateTuple;
        private final MutableMap<StateTuple<S>, Pair<StateTuple<S>, Twin<S>>> visitRecord;
        private final Queue<StateTuple<S>> pendingChecks;

        private CounterexampleBFS(FSA<Pair<S, S>> nonfinalSched, FSA<S> invariant, FSA<Pair<S, S>> rhs)
        {
            epsilon = invariant.alphabet().epsilon();
            epsilonPair = nonfinalSched.alphabet().epsilon();
            this.nonfinalScheduler = nonfinalSched;
            this.invariant = invariant;
            this.rhs = rhs.determinize().complete();
            startStateTuple = new StateTuple<>(nonfinalSched.startState(), invariant.startState(), rhs.startState());
            visitRecord = UnifiedMap.newMap(nonfinalSched.states().size() * rhs.states().size()); // heuristic
            pendingChecks = new LinkedList<>();
        }

        private void visit(StateTuple<S> target, StateTuple<S> from, Twin<S> withSymbol)
        {
            visitRecord.computeIfAbsent(target, pair -> {
                pendingChecks.add(pair);
                return Tuples.pair(from, withSymbol);
            });
        }

        private ListIterable<Twin<S>> witnessFoundAt(StateTuple<S> stateTuple, Twin<S> breakingStep)
        {
            final MutableList<Twin<S>> witnessBacktrace = FastList.newList();

            if (breakingStep != epsilon) {
                witnessBacktrace.add(breakingStep);
            }
            StateTuple<S> currStateTuple = stateTuple;
            Twin<S> currSymbol;
            while (!currStateTuple.equals(startStateTuple)) {
                final Pair<StateTuple<S>, Twin<S>> visitorAndSymbol = visitRecord.get(currStateTuple);
                if (!(currSymbol = visitorAndSymbol.getTwo()).equals(epsilon)) {
                    witnessBacktrace.add(currSymbol);
                }
                currStateTuple = visitorAndSymbol.getOne();
            }

            return witnessBacktrace.reverseThis();
        }

        private ListIterable<Twin<S>> run()
        {
            boolean invAccepts, rhsAccepts;
            invAccepts = invariant.isAcceptState(invariant.startState());
            rhsAccepts = rhs.isAcceptState(rhs.startState());
            if (nonfinalScheduler.isAcceptState(nonfinalScheduler.startState()) && invAccepts && !rhsAccepts) {
                return Lists.immutable.empty();
            }

            visit(startStateTuple, null, null);
            StateTuple<S> currStateTuple;
            State<S> invDept, invDest;
            State<Pair<S, S>> schedDept, rhsDept, rhsDest;
            while ((currStateTuple = pendingChecks.poll()) != null) {
                schedDept = currStateTuple.sched;
                invDept = currStateTuple.inv;
                rhsDept = currStateTuple.rhs;
                if (rhsDept.enabledSymbols().contains(epsilon)) {
                    throw new IllegalStateException("RHS should be deterministic");
                }
                for (Pair<S, S> symbol : schedDept.enabledSymbols()) {
                    if (!invDept.transitionExists(symbol.getOne())) {
                        continue;
                    }
                    invDest = symbol.equals(epsilonPair) ? invDept : invDept.successor(symbol.getOne());
                    rhsDest = symbol.equals(epsilonPair) ? rhsDept : rhsDept.successor(symbol);
                    rhsAccepts = !rhs.isAcceptState(rhsDest);
                    for (State<Pair<S, S>> schedDest : schedDept.successors(symbol)) {
                        if (nonfinalScheduler.isAcceptState(schedDest) && invAccepts && !rhsAccepts) {
                            return witnessFoundAt(currStateTuple, (Twin<S>) symbol);
                        }
                        visit(new StateTuple<>(schedDest, invDest, rhsDest), currStateTuple, (Twin<S>) symbol);
                    }
                }
            }

            return null;
        }
    }
}
