package core.proof;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.Problem;
import api.proof.Prover;
import common.sat.Sat4jSolverAdapter;
import common.sat.SatSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

public abstract class AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();

    protected final FSA<S> initialConfigs;
    protected final FSA<S> finalConfigs;
    protected final FSA<S> nonfinalConfigs;
    protected final FSA<S> schedulerDomain;
    protected final FSA<S> processRange;
    protected final FSA<Twin<S>> scheduler;
    protected final FSA<Twin<S>> process;
    protected final FSA<S> givenInvariant;
    protected final FSA<Twin<S>> givenOrder;
    protected final Alphabet<S> wholeAlphabet;
    protected final Alphabet<S> roundAlphabet;
    protected final Alphabet<Twin<S>> orderAlphabet;
    protected final ImmutableSet<Twin<S>> orderReflexiveSymbols;

    protected int invariantSizeBegin;
    protected final int invariantSizeEnd;
    protected int orderSizeBegin;
    protected final int orderSizeEnd;
    protected final boolean loosenInvariant;
    protected final boolean shapeInvariant;
    protected final boolean shapeOrder;

    protected final SatSolver solver;

    public AbstractProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder, boolean loosenInvariant)
    {
        initialConfigs = FSAs.minimize(FSAs.determinize(problem.initialConfigs()));
        finalConfigs = FSAs.minimize(FSAs.determinize(problem.finalConfigs()));
        nonfinalConfigs = FSAs.complement(finalConfigs);
        scheduler = problem.scheduler();
        process = problem.process();
        givenInvariant = problem.invariant();
        givenOrder = problem.order();

        wholeAlphabet = initialConfigs.alphabet(); // relying on current parsing behavior
        final ImmutableSet<S> roundSymbols = problem.process().transitionGraph().referredArcLabels()
                                                    .collect(Twin::getTwo);
        roundAlphabet = Alphabets.create(roundSymbols.newWith(wholeAlphabet.epsilon()), wholeAlphabet.epsilon());
        orderAlphabet = Alphabets.product(roundAlphabet);
        orderReflexiveSymbols = roundAlphabet.asSet().collect(s -> Tuples.twin(s, s));

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

        schedulerDomain = FSAs.project(scheduler, wholeAlphabet, Twin::getOne);
        processRange = FSAs.minimize(FSAs.determinize(FSAs.project(process, wholeAlphabet, Twin::getTwo)));
    }

    private LanguageSubsetChecker.Result<S> schedulerOperatesOnAllNonfinals()
    {
        final FSA<S> nonEmptyConfigs = FSAs.complement(FSAs.thatAcceptsOnly(wholeAlphabet, Lists.immutable.empty()));
        final FSA<S> nonEmptyNonfinalConfigs = FSAs.intersect(nonEmptyConfigs, nonfinalConfigs);

        return FSAs.checkSubset(nonEmptyNonfinalConfigs, schedulerDomain);
    }

    private LanguageSubsetChecker.Result<S> schedulerRespondsToAllProcesses()
    {
        return FSAs.checkSubset(FSAs.intersect(processRange, nonfinalConfigs), schedulerDomain);
    }

    protected LanguageSubsetChecker.Result<S> schedulerOperatesOnAllNonfinalInvariants(FSA<S> invariant)
    {
        return FSAs.checkSubset(FSAs.intersect(invariant, nonfinalConfigs), schedulerDomain);
    }

    protected void search(IntIntToObjectFunction<Pair<FSA<S>, FSA<Twin<S>>>> resultSupplier)
    {
        LOGGER.info("Scheduler operates on all nonfinals: {}", this::schedulerOperatesOnAllNonfinals);
        LOGGER.info("Scheduler responds to all process nonfinals: {}", this::schedulerRespondsToAllProcesses);

        Pair<FSA<S>, FSA<Twin<S>>> result;
        final int stabilizerBound = invariantSizeEnd * invariantSizeEnd + orderSizeEnd * orderSizeEnd;
        final long startTime = System.currentTimeMillis();
        for (int stabilizer = 1; stabilizer <= stabilizerBound; stabilizer++) {
            for (int invSize = invariantSizeBegin; invSize <= invariantSizeEnd; invSize++) {
                for (int ordSize = orderSizeBegin; ordSize <= orderSizeEnd; ordSize++) {
                    final int stabilizerFactor = invSize * invSize + ordSize * ordSize;
                    if (stabilizerFactor != stabilizer) {
                        continue;
                    }
                    if ((result = resultSupplier.value(invSize, ordSize)) != null) {
                        final long endTime = System.currentTimeMillis();
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
        final long endTime = System.currentTimeMillis();
        final long timeSpent = endTime - startTime;
        System.out.println("No proof found under the search bound.  " + timeSpent + "ms spent.");
    }
}
