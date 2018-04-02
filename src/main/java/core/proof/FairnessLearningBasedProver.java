package core.proof;

import api.automata.fsa.FSA;
import api.automata.fsa.LStarLearning;
import api.automata.fsa.LStarLearnings;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fst.FST;
import api.proof.Problem;
import api.proof.Prover;
import common.util.InterruptException;
import common.util.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import static common.util.Constants.DISPLAY_INDENT;
import static common.util.Constants.NOT_IMPLEMENTED_YET;
import static core.proof.CAV16MonoProver.checkBehaviorEnclosure;
import static core.proof.CAV16MonoProver.checkInitConfigsEnclosure;

public class FairnessLearningBasedProver<S> extends AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final FST<S, S> allBehavior;
    private ListIterable<S> counterexample;

    public FairnessLearningBasedProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder,
        boolean loosenInvariant)
    {
        super(problem, shapeInvariant, shapeOrder, loosenInvariant);

        allBehavior = scheduler.compose(process, orderAlphabet);
    }

    @Override
    public void prove()
    {
        final var startTime = Stopwatch.currentThreadCpuTimeInMs();
        try {
            final var teacher = new InvariantOracle();
            final var invariantOverApprox = LStarLearnings.learner().learn(roundAlphabet, teacher);
            final var timeSpent = Stopwatch.currentThreadCpuTimeInMs() - startTime;
            System.out.println("A proof found under the search bound in " + timeSpent + "ms.");
            System.out.println();
            System.out.println("InvariantOver " + invariantOverApprox);
            System.out.println("PreStarFinalUnderCovering: " + teacher.currPreStarSteppingCoverage + " steps");
            System.out.println("PreStarFinalUnderConverge: " + teacher.preStarFinalConverge);
            System.out.println();
            System.out.println("PreStarFinalUnder " + teacher.currPreStarFinalUnderApprox);
        } catch (ProofCounterexampleFound e) {
            final var timeSpent = Stopwatch.currentThreadCpuTimeInMs() - startTime;
            System.out.println("A counterexample found in the precise invariant in " + timeSpent + "ms:");
            System.out.print(DISPLAY_INDENT + "-- " + counterexample);
        } catch (InterruptException e) {
            // should not happen
        }
    }

    @Override
    public void verify()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    private class InvariantOracle implements LStarLearning.Teacher<S>
    {
        private final MutableIntObjectMap<FSA<S>> preciseInvariantScopeOnLength;
        private FST<S, S> currPreStarStepping;
        private FSA<S> currPreStarFinalUnderApprox;
        private int currPreStarSteppingCoverage;
        private boolean preStarFinalConverge;
        private long profilingStartTime;

        private InvariantOracle()
        {
            preciseInvariantScopeOnLength = new IntObjectHashMap<>();
            currPreStarFinalUnderApprox = finalConfigs;
            currPreStarSteppingCoverage = 0;
        }

        @Override
        public boolean targetAccepts(ListIterable<S> word)
        {
            final var length = word.size();

            return preciseInvariantScopeOnLength.getIfAbsentPut(length, () -> {
                final var result = allBehavior.postStarImageOnLength(initialConfigs, length);
                LOGGER.info(
                    () -> "Precise invariant on length " + length + " computed in " + result.getTwo() + " steps.");
                return result.getOne();
            }).accepts(word);
        }

        private void extendPreStarFinalUnderApprox()
        {
            LOGGER.info("Extend Pre*(F) under at thread time {}ms.", //
                        () -> (profilingStartTime = Stopwatch.currentThreadCpuTimeInMs()));
            if (currPreStarSteppingCoverage == 0) {
                currPreStarStepping = allBehavior.inverse();
                currPreStarSteppingCoverage = 1;
            } else {
                currPreStarStepping = currPreStarStepping.compose(currPreStarStepping, currPreStarStepping.alphabet())
                                                         .union(allBehavior.inverse()); // cover step 2^{0 ~ i}
                currPreStarSteppingCoverage = currPreStarSteppingCoverage * 2;
            }
            final var extended = currPreStarStepping.postImage(currPreStarFinalUnderApprox) // step 2^{0 ~ i}
                                                    .union(finalConfigs); // step 0
            if (currPreStarFinalUnderApprox.checkContaining(extended).passed()) {
                preStarFinalConverge = true;
            } else {
                currPreStarFinalUnderApprox = extended.determinize();
            }
            LOGGER.info(() -> "Pre*(F) under " + currPreStarSteppingCoverage + " steps (" +
                (preStarFinalConverge ? "" : "yet ") + "converged) computed in " +
                (Stopwatch.currentThreadCpuTimeInMs() - profilingStartTime) + "ms.");
        }

        @Override
        public LStarLearning.EquivalenceCheckResult<S> checkAnswer(FSA<S> answer) throws ProofCounterexampleFound
        {
            final var l1 = checkInitConfigsEnclosure(initialConfigs, answer);
            if (l1.rejected()) {
                LOGGER.debug("Initial configurations enclosed: {}", l1);
                return new AnswerCheckResult(false, l1.counterexample().witness(), null);
            }

            final var l2 = checkBehaviorEnclosure(allBehavior, answer);
            if (l2.rejected()) {
                LOGGER.debug("Transition behavior enclosed: {}", l2);
                final var breakingStep = l2.counterexample().breakingStep();
                if (targetAccepts(breakingStep)) {
                    return new AnswerCheckResult(false, breakingStep, null);
                }

                final var causeOfBreaking = l2.counterexample().causes().getFirst();

                return new AnswerCheckResult(false, null, causeOfBreaking);
            }

            LanguageSubsetChecker.Result<S> l3;
            while (true) {
                if ((l3 = currPreStarFinalUnderApprox.checkContaining(answer)).passed()) {
                    return new AnswerCheckResult(true, null, null);
                }

                final var configToCheck = l3.counterexample().witness();
                if (!targetAccepts(configToCheck)) {
                    return new AnswerCheckResult(false, null, configToCheck);
                }

                LOGGER.debug("Invariant over touches Pre*(F) under at: {}", configToCheck);
                if (!preStarFinalConverge && configToCheck.size() > currPreStarSteppingCoverage) {
                    extendPreStarFinalUnderApprox();
                }
                if (!currPreStarFinalUnderApprox.accepts(configToCheck)) {
                    counterexample = configToCheck;
                    throw new ProofCounterexampleFound();
                }
            }
        }
    }

    private class AnswerCheckResult implements LStarLearning.EquivalenceCheckResult<S>
    {
        private final boolean passed;
        private final ListIterable<S> positiveCounterexample;
        private final ListIterable<S> negativeCounterexample;

        private AnswerCheckResult(boolean passed, ListIterable<S> positiveCounterexample,
            ListIterable<S> negativeCounterexample)
        {
            this.passed = passed;
            this.positiveCounterexample = positiveCounterexample;
            this.negativeCounterexample = negativeCounterexample;
        }

        @Override
        public boolean passed()
        {
            return passed;
        }

        @Override
        public ListIterable<S> positiveCounterexample()
        {
            return positiveCounterexample;
        }

        @Override
        public ListIterable<S> negativeCounterexample()
        {
            return negativeCounterexample;
        }
    }

    private static class ProofCounterexampleFound extends InterruptException
    {
    }
}
