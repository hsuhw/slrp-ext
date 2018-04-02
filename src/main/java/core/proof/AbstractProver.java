package core.proof;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fsa.MutableFSA;
import api.automata.fst.FST;
import api.proof.Problem;
import api.proof.Prover;
import common.sat.Sat4jSolverAdapter;
import common.sat.SatSolver;
import common.util.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

public abstract class AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();

    final FSA<S> initialConfigs;
    final FSA<S> finalConfigs;
    final FSA<S> nonfinalConfigs;
    final FST<S, S> scheduler;
    private final FSA<S> schedulerDomain;
    final FST<S, S> process;
    private final FSA<S> processRange;
    final FSA<S> givenInvariant;
    final FST<S, S> givenOrder;
    private final Alphabet<S> wholeAlphabet;
    final Alphabet<S> roundAlphabet;
    final Alphabet<Pair<S, S>> orderAlphabet;
    final SetIterable<Pair<S, S>> orderReflexiveSymbols;

    int invariantSizeBegin;
    private final int invariantSizeEnd;
    int orderSizeBegin;
    private final int orderSizeEnd;
    final boolean loosenInvariant;
    final boolean shapeInvariant;
    final boolean shapeOrder;

    protected final SatSolver solver;

    AbstractProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder, boolean loosenInvariant)
    {
        nonfinalConfigs = problem.finalConfigs().determinize().minimize().complement();
        scheduler = problem.scheduler();
        process = problem.process();
        wholeAlphabet = problem.initialConfigs().alphabet(); // relying on current parsing behavior
        schedulerDomain = ((MutableFSA<S>) scheduler.domain()).setAlphabet(wholeAlphabet).determinize();
        processRange = ((MutableFSA<S>) process.range()).setAlphabet(wholeAlphabet).determinize().minimize();
        givenInvariant = problem.invariant();
        givenOrder = problem.order();

        final var roundSymbols = process.transitionGraph().referredArcLabels().collect(Pair::getTwo).toSet();
        roundSymbols.add(wholeAlphabet.epsilon());
        roundAlphabet = Alphabets.create(roundSymbols, wholeAlphabet.epsilon());
        orderAlphabet = Alphabets.product(roundAlphabet, roundAlphabet);
        orderReflexiveSymbols = roundAlphabet.asSet().collect(s -> Tuples.pair(s, s)).toSet();
        finalConfigs = ((MutableFSA<S>) problem.finalConfigs()).setAlphabet(roundAlphabet).determinize().minimize();
        initialConfigs = ((MutableFSA<S>) problem.initialConfigs()).setAlphabet(roundAlphabet).determinize().minimize();

        final var invSizeBound = problem.invariantSizeBound();
        final var ordSizeBound = problem.orderSizeBound();
        invariantSizeBegin = invSizeBound != null ? invSizeBound.getOne() : 0;
        invariantSizeEnd = invSizeBound != null ? invSizeBound.getTwo() : 0;
        orderSizeBegin = ordSizeBound != null ? ordSizeBound.getOne() : 0;
        orderSizeEnd = ordSizeBound != null ? ordSizeBound.getTwo() : 0;
        this.loosenInvariant = loosenInvariant;
        this.shapeInvariant = shapeInvariant;
        this.shapeOrder = shapeOrder;

        solver = new Sat4jSolverAdapter();
    }

    private LanguageSubsetChecker.Result<S> schedulerOperatesOnAllNonfinals()
    {
        final var nonEmptyConfigs = FSAs.acceptingOnly(wholeAlphabet, Lists.immutable.of(Lists.immutable.empty()));
        final var nonEmptyNonfinalConfigs = nonEmptyConfigs.intersect(nonfinalConfigs);

        return schedulerDomain.checkContaining(nonEmptyNonfinalConfigs);
    }

    private LanguageSubsetChecker.Result<S> schedulerRespondsToAllProcesses()
    {
        return schedulerDomain.checkContaining(processRange.intersect(nonfinalConfigs));
    }

    LanguageSubsetChecker.Result<S> schedulerOperatesOnAllNonfinalInvariants(FSA<S> invariant)
    {
        return schedulerDomain.checkContaining(invariant.intersect(nonfinalConfigs));
    }

    void search(IntIntToObjectFunction<Pair<FSA<S>, FST<S, S>>> resultSupplier)
    {
        LOGGER.info("Scheduler operates on all nonfinals: {}", this::schedulerOperatesOnAllNonfinals);
        LOGGER.info("Scheduler responds to all process nonfinals: {}", this::schedulerRespondsToAllProcesses);

        Pair<FSA<S>, FST<S, S>> result;
        final var stabilizerBound = invariantSizeEnd * invariantSizeEnd + orderSizeEnd * orderSizeEnd;
        final var startTime = Stopwatch.currentThreadCpuTimeInMs();
        for (var stabilizer = 1; stabilizer <= stabilizerBound; stabilizer++) {
            for (var invSize = invariantSizeBegin; invSize <= invariantSizeEnd; invSize++) {
                for (var ordSize = orderSizeBegin; ordSize <= orderSizeEnd; ordSize++) {
                    final var stabilizerFactor = invSize * invSize + ordSize * ordSize;
                    if (stabilizerFactor != stabilizer) {
                        continue;
                    }
                    if ((result = resultSupplier.value(invSize, ordSize)) != null) {
                        final var endTime = Stopwatch.currentThreadCpuTimeInMs();
                        final var timeSpent = endTime - startTime;
                        System.out.println("A proof found under the search bound in " + timeSpent + "ms.");
                        System.out.println();
                        System.out.println("A " + result.getOne());
                        System.out.println("T (>) " + result.getTwo());
                        return;
                    }
                }
            }
        }
        final var endTime = Stopwatch.currentThreadCpuTimeInMs();
        final var timeSpent = endTime - startTime;
        System.out.println("No proof found under the search bound.  " + timeSpent + "ms spent.");
    }
}
