package core.proof;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.common.sat.ContradictionException;
import api.common.sat.SatSolver;
import api.proof.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.proof.FSAEncoding.CertainWord;
import static api.util.Values.DISPLAY_NEWLINE;

public class CAV16MonoProver<S> extends AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BehaviorEnclosureChecker BEHAVIOR_ENCLOSURE_CHECKER;
    private static final TransitivityChecker TRANSITIVITY_CHECKER;
    private static final AnySchedulerProgressabilityChecker ANY_SCHEDULER_PROGRESSABILITY_CHECKER;

    private final FSA<Twin<S>> nfScheduler;
    private final FSA<Twin<S>> allBehavior;
    private final boolean invEnclosesAll;

    static {
        BEHAVIOR_ENCLOSURE_CHECKER = new BasicBehaviorEnclosureChecker();
        TRANSITIVITY_CHECKER = new BasicTransitivityChecker();
        ANY_SCHEDULER_PROGRESSABILITY_CHECKER = new BasicAnySchedulerProgressabilityChecker();
    }

    private static <S> FSA<Twin<S>> makeNonfinalScheduler(FSA<Twin<S>> sched, FSA<S> nfConfigs)
    {
        return Transducers.filterByOutput(Transducers.filterByInput(sched, nfConfigs), nfConfigs);
    }

    public CAV16MonoProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder, boolean loosenInvariant)
    {
        super(problem, shapeInvariant, shapeOrder, loosenInvariant);

        nfScheduler = makeNonfinalScheduler(scheduler, nonfinalConfigs);
//        allBehavior = loosenInvariant
//                      ? FSAs.union(scheduler, process)
//                      : Transducers.compose(scheduler, process, scheduler.alphabet());
        allBehavior = Transducers.compose(scheduler, process, scheduler.alphabet());
        invEnclosesAll = problem.invariantEnclosesAllBehavior();
    }

    static <S> FSAEncoding<S> newFSAEncoding(SatSolver solver, int size, AlphabetIntEncoder<S> alphabetEncoding,
                                             boolean restrictShape)
    {
        FSAEncoding<S> instance = new BasicFSAEncoding<>(solver, size, alphabetEncoding, restrictShape);
        instance.ensureNoDanglingState();

        return instance;
    }

    static <S> LanguageSubsetChecker.Result<S> checkInitConfigsEnclosure(FSA<S> initConfigs, FSA<S> encloser)
    {
        return FSAs.checkSubset(initConfigs, encloser);
    }

    static <S> void refineInitConfigsEncloser(FSAEncoding<S> encloserEncoding,
                                              LanguageSubsetChecker.Counterexample<S> counterexample)
    {
        encloserEncoding.ensureAccepting(counterexample.get());
    }

    static <S> BehaviorEnclosureChecker.Result<S> checkBehaviorEnclosure(FSA<Twin<S>> behavior, FSA<S> encloser)
    {
        return BEHAVIOR_ENCLOSURE_CHECKER.test(behavior, encloser);
    }

    static <S> void refineBehaviorEncloser(SatSolver solver, FSAEncoding<S> encloserEncoding,
                                           BehaviorEnclosureChecker.Counterexample<S> counterexample)
    {
        final ImmutableList<S> witness = counterexample.get();
        final int takenWitness = solver.newFreeVariable();
        encloserEncoding.ensureAcceptingIfOnlyIf(takenWitness, witness);
        for (ImmutableList<S> cause : counterexample.causes()) {
            final int takenCause = solver.newFreeVariable();
            encloserEncoding.ensureAcceptingIfOnlyIf(takenCause, cause);
            solver.addImplication(takenCause, takenWitness);
        }
    }

    static <S> TransitivityChecker.Result<S> checkTransitivity(FSA<Twin<S>> target)
    {
        return TRANSITIVITY_CHECKER.test(target);
    }

    static <S> void refineTransitivity(SatSolver solver, FSAEncoding<Twin<S>> targetEncoding,
                                       TransitivityChecker.Counterexample<S> counterexample)
    {
        final int takenXZ = solver.newFreeVariable();
        targetEncoding.ensureAcceptingIfOnlyIf(takenXZ, counterexample.get());
        for (Twin<ImmutableList<Twin<S>>> cause : counterexample.causes()) {
            final int takenXY = solver.newFreeVariable();
            final int takenYZ = solver.newFreeVariable();
            targetEncoding.ensureAcceptingIfOnlyIf(takenXY, cause.getOne());
            targetEncoding.ensureAcceptingIfOnlyIf(takenYZ, cause.getTwo());
            solver.addClause(-takenXY, -takenYZ, takenXZ);
        }
    }

    static <S> AnySchedulerProgressabilityChecker.Result<S> checkProgressability(FSA<Twin<S>> nfScheduler,
                                                                                 FSA<Twin<S>> process, FSA<S> invariant,
                                                                                 FSA<Twin<S>> order)
    {
        return ANY_SCHEDULER_PROGRESSABILITY_CHECKER.test(nfScheduler, process, invariant, order);
    }

    static <S> void refineProgressability(SatSolver solver, FSAEncoding<S> invariantEncoding,
                                          FSAEncoding<Twin<S>> orderEncoding, FSA<Twin<S>> process,
                                          Alphabet<S> steadyAlphabet,
                                          AnySchedulerProgressabilityChecker.Counterexample<S> counterexample)
    {
        final ImmutableList<S> x = counterexample.get().collect(Twin::getOne);
        final ImmutableList<S> y = counterexample.get().collect(Twin::getTwo);
        if (y.isEmpty() || y.get(0) == null) {
            LOGGER.debug("Blocking {}", x);
            invariantEncoding.ensureNoAccepting(x);
            return;
        }
        final FSA<S> possibleZ = FSAs.thatAcceptsOnly(steadyAlphabet, Transducers.postImage(process, y));
        if (possibleZ.acceptsNone()) {
            LOGGER.debug("Blocking {}", x);
            invariantEncoding.ensureNoAccepting(x);
            return;
        }

        final int takenX = solver.newFreeVariable();
        invariantEncoding.ensureAcceptingIfOnlyIf(takenX, x);
        final int shouldBeCertainZ = solver.newFreeVariable();
        final CertainWord<S> z = invariantEncoding.ensureAcceptingCertainWordIf(shouldBeCertainZ, x.size());
        z.ensureAcceptedBy(possibleZ);
        final CertainWord<Twin<S>> xz = orderEncoding.ensureAcceptingCertainWordIf(shouldBeCertainZ, x.size());
        final ImmutableSet<S> noEpsilonSteadyAlphabet = steadyAlphabet.noEpsilonSet();
        x.forEachWithIndex((chx, pos) -> {
            noEpsilonSteadyAlphabet.forEach(chz -> {
                final Twin<S> chxz = Tuples.twin(chx, chz);
                final int zHasChzAtPos = z.getCharacterIndicator(pos, chz);
                final int xzHasChxzAtPos = xz.getCharacterIndicator(pos, chxz);
                solver.addImplication(zHasChzAtPos, xzHasChxzAtPos);
            });
        });

        solver.addImplication(takenX, shouldBeCertainZ);
    }

    @Override
    public void prove()
    {
        final AlphabetIntEncoder<S> invSymbolEncoding = AlphabetIntEncoders.create(wholeAlphabet);
        final AlphabetIntEncoder<Twin<S>> ordSymbolEncoding = AlphabetIntEncoders.create(orderAlphabet);

        // having empty string excluded makes searching from 0 or 1 meaningless
        invariantSizeBegin = invariantSizeBegin < 1 ? 1 : invariantSizeBegin;
        orderSizeBegin = orderSizeBegin < 2 ? 2 : orderSizeBegin;

        search((invSize, ordSize) -> {
            LOGGER.info("Searching in state spaces {} & {} ..", invSize, ordSize);

            final FSAEncoding<S> invEnc = newFSAEncoding(solver, invSize, invSymbolEncoding, shapeInvariant);
            final FSAEncoding<Twin<S>> ordEnc = newFSAEncoding(solver, ordSize, ordSymbolEncoding, shapeOrder);
            ordEnc.ensureNoWordPurelyMadeOf(orderReflexiveSymbols);

            LanguageSubsetChecker.Result<S> l1;
            BehaviorEnclosureChecker.Result<S> l2 = null;
            TransitivityChecker.Result<S> l3;
            LanguageSubsetChecker.Result<S> l4Precheck;
            AnySchedulerProgressabilityChecker.Counterexample<S> l4PrecheckViolation;
            AnySchedulerProgressabilityChecker.Result<S> l4;
            while (solver.findItSatisfiable()) {
                boolean contradiction = false;
                final FSA<S> invCand = invEnc.resolve();
                final FSA<Twin<S>> ordCand = ordEnc.resolve();

                LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
                LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

                if ((l1 = checkInitConfigsEnclosure(initialConfigs, invCand)).rejected()) {
                    LOGGER.debug("Initial configurations enclosed: {}", l1);
                    refineInitConfigsEncloser(invEnc, l1.counterexample());
                }
                if (invEnclosesAll && (l2 = checkBehaviorEnclosure(allBehavior, invCand)).rejected()) {
                    LOGGER.debug("Transition behavior enclosed: {}", l2);
                    refineBehaviorEncloser(solver, invEnc, l2.counterexample());
                }
                if ((l3 = checkTransitivity(ordCand)).rejected()) {
                    LOGGER.debug("Strict pre-order relation: {}", l3);
                    refineTransitivity(solver, ordEnc, l3.counterexample());
                }
                if (!loosenInvariant && (l4Precheck = schedulerOperatesOnAllNonfinalInvariants(invCand)).rejected()) {
                    final ImmutableList<S> violation = l4Precheck.counterexample().get();
                    final ImmutableList<Twin<S>> v = violation.collect(ch -> Tuples.twin(ch, null));
                    l4PrecheckViolation = new BasicAnySchedulerProgressabilityChecker.Counterexample<>(v);
                    l4 = new BasicAnySchedulerProgressabilityChecker.Result<>(false, l4PrecheckViolation);
                } else {
                    l4 = checkProgressability(nfScheduler, process, invCand, ordCand);
                }
                if (l4.rejected()) {
                    LOGGER.debug("Progressability: {}", l4);
                    try {
                        refineProgressability(solver, invEnc, ordEnc, process, roundAlphabet, l4.counterexample());
                    } catch (ContradictionException e) {
                        contradiction = true;
                    }
                }

                LOGGER.info("Rules checked: {} {} {} {}", //
                            l1.passed(), invEnclosesAll ? l2.passed() : "--", l3.passed(), l4.passed());
                if (l1.passed() && (!invEnclosesAll || l2.passed()) && l3.passed() && l4.passed()) {
                    return Tuples.pair(invCand, ordCand);
                }
                if (contradiction) {
                    break;
                }
            }

            solver.reset();
            return null;
        });
    }

    @Override
    public void verify()
    {
        final FSA<S> invCand = FSAs.determinize(givenInvariant);
        final FSA<Twin<S>> ordCand = FSAs.determinize(givenOrder);

        final String l1 = checkInitConfigsEnclosure(initialConfigs, invCand).toString();
        final String l2 = invEnclosesAll ? checkBehaviorEnclosure(allBehavior, invCand).toString() : "--";
        final String l3 = checkTransitivity(ordCand).toString();
        final String l4 = checkProgressability(nfScheduler, process, invCand, ordCand).toString();

        LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
        LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

        System.out.println("Initial configurations enclosed: " + l1);
        System.out.println("All behavior enclosed: " + l2);
        System.out.println("Strict pre-order relation: " + l3);
        System.out.println("Progressability: " + l4);
    }
}
