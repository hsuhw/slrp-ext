package core.proof;

import api.automata.Alphabet;
import api.automata.State;
import api.automata.States;
import api.automata.TransitionGraph;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.proof.AnySchedulerProgressabilityChecker;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.Queue;

import static api.automata.AutomatonManipulator.makeStartAndAcceptStates;
import static api.util.Connectives.AND;
import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;

public class BasicAnySchedulerProgressabilityChecker implements AnySchedulerProgressabilityChecker
{
    @Override
    public <S> Result<S> test(FSA<Twin<S>> nfSched, FSA<Twin<S>> proc, FSA<S> nfConfigs, FSA<S> inv, FSA<Twin<S>> order)
    {
        final FSA<Twin<S>> procInInv = Transducers.filterByOutput(proc, inv);
        final FSA<Twin<S>> progressAvailSchedMoves = FSAs.product(procInInv, order, proc.alphabet(), (trans, ord) -> {
            return trans.getTwo().equals(ord.getTwo()) ? Tuples.twin(ord.getOne(), trans.getOne()) : null;
        }, makeStartAndAcceptStates(procInInv, order, AND, AND));
        final CounterexampleFinder<S> finder = new CounterexampleFinder<>(nfSched, inv, progressAvailSchedMoves);
        final FSA<Twin<S>> counterexampleImage = finder.findObservingImage();

        if (counterexampleImage.acceptsNone()) {
            return new Result<>(true, null);
        }

        return new Result<>(false, new Counterexample<>(counterexampleImage.enumerateOneShortest()));
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
        private ImmutableList<Twin<S>> instance;

        private Counterexample(ImmutableList<Twin<S>> instance)
        {
            this.instance = instance;
        }

        @Override
        public ImmutableList<Twin<S>> get()
        {
            return instance;
        }

        @Override
        public String toString()
        {
            return "witness of non-progressable parts: " + get();
        }
    }

    private class StateTuple
    {
        private final State nfSched;
        private final State inv;
        private final State rhs;
        private int hashCode = -1;

        private StateTuple(State nfSched, State inv, State rhs)
        {
            this.nfSched = nfSched;
            this.inv = inv;
            this.rhs = rhs;
        }

        @Override
        public int hashCode()
        {
            if (hashCode == -1) {
                final int prime = 67;
                int result = 1;

                result = prime * result + nfSched.hashCode();
                result = prime * result + inv.hashCode();
                result = prime * result + rhs.hashCode();

                hashCode = result;
            }

            return hashCode;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof StateTuple) {
                try {
                    @SuppressWarnings("unchecked")
                    final StateTuple other = (StateTuple) obj;
                    return other.nfSched.equals(this.nfSched) && other.inv.equals(this.inv) //
                        && other.rhs.equals(this.rhs);
                } catch (ClassCastException e) {
                    return false;
                }
            }
            return false;
        }
    }

    private class CounterexampleFinder<S>
    {
        private final FSA<Twin<S>> nfSched;
        private final FSA<S> inv;
        private final FSA<Twin<S>> rhs;
        private final TransitionGraph<State, Twin<S>> nfSchedDelta;
        private final TransitionGraph<State, S> invDelta;
        private final TransitionGraph<State, Twin<S>> rhsDelta;
        private final int imageStateCapacity;
        private final FSA.Builder<Twin<S>> builder;
        private final MutableBiMap<StateTuple, State> stateMapping;
        private final Queue<StateTuple> pendingChecks;

        private CounterexampleFinder(FSA<Twin<S>> nfSched, FSA<S> inv, FSA<Twin<S>> rhs)
        {
            this.nfSched = nfSched;
            nfSchedDelta = nfSched.transitionGraph();
            this.inv = inv;
            invDelta = inv.transitionGraph();
            this.rhs = FSAs.complete(FSAs.determinize(rhs));
            rhsDelta = this.rhs.transitionGraph();
            imageStateCapacity = nfSched.states().size() * rhs.states().size(); // heuristic
            final Alphabet<Twin<S>> alphabet = nfSched.alphabet();
            builder = FSAs.builder(imageStateCapacity, alphabet.size(), alphabet.epsilon());
            stateMapping = new HashBiMap<>(imageStateCapacity);
            pendingChecks = new LinkedList<>();
        }

        private State takeState(StateTuple stateTuple)
        {
            return stateMapping.computeIfAbsent(stateTuple, tuple -> {
                pendingChecks.add(tuple);
                return States.generate();
            });
        }

        private State takeState(State nfSched, State inv, State rhs)
        {
            return takeState(new StateTuple(nfSched, inv, rhs));
        }

        private boolean lhsAcceptsButRhsDoesNot(StateTuple currState)
        {
            return nfSched.isAcceptState(currState.nfSched) && inv.isAcceptState(currState.inv) //
                && !rhs.isAcceptState(currState.rhs);
        }

        private FSA<Twin<S>> findObservingImage()
        {
            final State start = takeState(nfSched.startState(), inv.startState(), rhs.startState());
            builder.addStartState(start);

            StateTuple currTuple;
            while ((currTuple = pendingChecks.poll()) != null) {
                if (lhsAcceptsButRhsDoesNot(currTuple)) {
                    builder.addAcceptState(stateMapping.get(currTuple));
                    break;
                }
                final State schedDept = currTuple.nfSched;
                final State invDept = currTuple.inv;
                final State rhsDept = currTuple.rhs;
                for (State dest : nfSchedDelta.directSuccessorsOf(schedDept, nfSched.alphabet().epsilon())) {
                    builder.addEpsilonTransition(takeState(currTuple), takeState(dest, invDept, rhsDept));
                }
                for (Twin<S> schedMove : nfSchedDelta.nonEpsilonArcLabelsFrom(schedDept)) {
                    final S x = schedMove.getOne();
                    if (!invDelta.arcLabeledFrom(invDept, x)) {
                        continue;
                    }
                    final State invDest = invDelta.directSuccessorOf(invDept, x);
                    final State rhsDest = rhsDelta.directSuccessorOf(rhsDept, schedMove);
                    for (State schedDest : nfSchedDelta.directSuccessorsOf(schedDept, schedMove)) {
                        builder.addTransition(takeState(currTuple), takeState(schedDest, invDest, rhsDest), schedMove);
                    }
                }
            }

            return builder.buildWith(nfSched.alphabet());
        }
    }
}
