package core.proof;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fst.FST;
import api.proof.Problem;
import api.proof.Prover;
import common.sat.Sat4jSolverAdapter;
import common.sat.SatSolver;
import common.util.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

public abstract class AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();

    final FSA<S> initialConfigs;
    final FSA<S> nonfinalConfigs;
    final FST<S, S> scheduler;
    private final FSA<S> schedulerDomain;
    final FST<S, S> process;
    private final FSA<S> processRange;
    final FSA<S> givenInvariant;
    final FST<S, S> givenOrder;
    final Alphabet<S> wholeAlphabet;
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
        initialConfigs = problem.initialConfigs().determinize().minimize();
        nonfinalConfigs = problem.finalConfigs().determinize().minimize().complement();
        scheduler = problem.scheduler();
        process = problem.process();
        schedulerDomain = scheduler.domain().determinize().minimize();
        processRange = process.range().determinize().minimize();
        givenInvariant = problem.invariant();
        givenOrder = problem.order();

        wholeAlphabet = initialConfigs.alphabet(); // relying on current parsing behavior
        final MutableSet<S> roundSymbols = process.transitionGraph().referredArcLabels().collect(Pair::getTwo).toSet();
        roundSymbols.add(wholeAlphabet.epsilon());
        roundAlphabet = Alphabets.create(roundSymbols, wholeAlphabet.epsilon());
        orderAlphabet = Alphabets.product(roundAlphabet, roundAlphabet);
        orderReflexiveSymbols = roundAlphabet.asSet().collect(s -> Tuples.pair(s, s)).toSet();

        final IntIntPair invSizeBound = problem.invariantSizeBound();
        final IntIntPair ordSizeBound = problem.orderSizeBound();
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
        final FSA<S> nonEmptyConfigs = FSAs.acceptingOnly(wholeAlphabet, Lists.immutable.of(Lists.immutable.empty()));
        final FSA<S> nonEmptyNonfinalConfigs = nonEmptyConfigs.intersect(nonfinalConfigs);

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
        final int stabilizerBound = invariantSizeEnd * invariantSizeEnd + orderSizeEnd * orderSizeEnd;
        final long startTime = Stopwatch.currentThreadCpuTimeInMs();
        for (int stabilizer = 1; stabilizer <= stabilizerBound; stabilizer++) {
            for (int invSize = invariantSizeBegin; invSize <= invariantSizeEnd; invSize++) {
                for (int ordSize = orderSizeBegin; ordSize <= orderSizeEnd; ordSize++) {
                    final int stabilizerFactor = invSize * invSize + ordSize * ordSize;
                    if (stabilizerFactor != stabilizer) {
                        continue;
                    }
                    if ((result = resultSupplier.value(invSize, ordSize)) != null) {
                        final long endTime = Stopwatch.currentThreadCpuTimeInMs();
                        final long timeSpent = endTime - startTime;
                        System.out.println("A proof found under the search bound in " + timeSpent + "ms.");
                        System.out.println();
                        System.out.println("A " + result.getOne());
                        System.out.println("T (>) " + result.getTwo());
                        return;
                    }
                }
            }
        }
        final long endTime = Stopwatch.currentThreadCpuTimeInMs();
        final long timeSpent = endTime - startTime;
        System.out.println("No proof found under the search bound.  " + timeSpent + "ms spent.");
    }
}
