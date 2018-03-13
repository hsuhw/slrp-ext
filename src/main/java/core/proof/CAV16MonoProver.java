package core.proof;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fsa.MutableFSA;
import api.automata.fst.FST;
import api.automata.fst.FSTs;
import api.proof.*;
import common.sat.ContradictionException;
import common.sat.SatSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.List;

import static api.proof.FSAEncoding.CertainWord;
import static common.util.Constants.DISPLAY_NEWLINE;

public class CAV16MonoProver<S> extends AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BehaviorEnclosureChecker BEHAVIOR_ENCLOSURE_CHECKER;
    private static final TransitivityChecker TRANSITIVITY_CHECKER;
    private static final AnySchedulerProgressivityChecker ANY_SCHEDULER_PROGRESSABILITY_CHECKER;

    private final FST<S, S> nonfinalScheduler;
    private final FST<S, S> allBehavior;
    private final boolean invEnclosesAll;

    static {
        BEHAVIOR_ENCLOSURE_CHECKER = new BasicBehaviorEnclosureChecker();
        TRANSITIVITY_CHECKER = new BasicTransitivityChecker();
        ANY_SCHEDULER_PROGRESSABILITY_CHECKER = new BasicAnySchedulerProgressivityChecker();
    }

    public CAV16MonoProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder, boolean loosenInvariant)
    {
        super(problem, shapeInvariant, shapeOrder, loosenInvariant);

        nonfinalScheduler = scheduler.maskByInput(nonfinalConfigs).maskByOutput(nonfinalConfigs);
//        allBehavior = loosenInvariant
//                      ? scheduler.union(process)
//                      : scheduler.compose(process, scheduler.alphabet());
        allBehavior = scheduler.compose(process, scheduler.alphabet());
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
        return encloser.checkContaining(initConfigs);
    }

    static <S> void refineInitConfigsEncloser(FSAEncoding<S> encloserEncoding,
        LanguageSubsetChecker.Counterexample<S> counterexample)
    {
        encloserEncoding.ensureAccepting(counterexample.witness());
    }

    static <S> BehaviorEnclosureChecker.Result<S> checkBehaviorEnclosure(FST<S, S> behavior, FSA<S> encloser)
    {
        return BEHAVIOR_ENCLOSURE_CHECKER.test(behavior, encloser);
    }

    static <S> void refineBehaviorEncloser(SatSolver solver, FSAEncoding<S> encloserEncoding,
        BehaviorEnclosureChecker.Counterexample<S> counterexample)
    {
        final ListIterable<S> invalidStep = counterexample.invalidStep();
        final int takenStep = solver.newFreeVariable();
        encloserEncoding.ensureAcceptingIfOnlyIf(takenStep, invalidStep);
        for (ListIterable<S> cause : counterexample.causes()) {
            final int takenCause = solver.newFreeVariable();
            encloserEncoding.ensureAcceptingIfOnlyIf(takenCause, cause);
            solver.addImplication(takenCause, takenStep);
        }
    }

    static <S> TransitivityChecker.Result<S> checkTransitivity(FST<S, S> target)
    {
        return TRANSITIVITY_CHECKER.test(target);
    }

    static <S> void refineTransitivity(SatSolver solver, FSAEncoding<Pair<S, S>> targetEncoding,
        TransitivityChecker.Counterexample<S> counterexample)
    {
        final ListIterable<Pair<S, S>> invalidStep = counterexample.invalidStep();
        final ListIterable<S> x = invalidStep.collect(Pair::getOne);
        final ListIterable<S> z = invalidStep.collect(Pair::getTwo);

        final int takenStep = solver.newFreeVariable();
        targetEncoding.ensureAcceptingIfOnlyIf(takenStep, invalidStep);
        for (ListIterable<S> y : counterexample.validMiddleSteps()) {
            final int takenXY = solver.newFreeVariable();
            final int takenYZ = solver.newFreeVariable();
            targetEncoding.ensureAcceptingIfOnlyIf(takenXY, Alphabets.pairWord(x, y));
            targetEncoding.ensureAcceptingIfOnlyIf(takenYZ, Alphabets.pairWord(y, z));
            solver.addClause(-takenXY, -takenYZ, takenStep);
        }
    }

    private static <S> AnySchedulerProgressivityChecker.Result<S> checkProgressivity(FST<S, S> nonfinalScheduler,
        FST<S, S> process, FSA<S> invariant, FST<S, S> order)
    {
        return ANY_SCHEDULER_PROGRESSABILITY_CHECKER.test(nonfinalScheduler, process, invariant, order);
    }

    private static <S> void refineProgressivity(SatSolver solver, FSAEncoding<S> invariantEncoding,
        FSAEncoding<Pair<S, S>> orderEncoding, FST<S, S> process, Alphabet<S> steadyAlphabet,
        AnySchedulerProgressivityChecker.Counterexample<S> counterexample)
    {
        final ListIterable<S> x = counterexample.fruitlessStep().collect(Twin::getOne);
        final ListIterable<S> y = counterexample.fruitlessStep().collect(Twin::getTwo);
        if (y.isEmpty() || y.get(0) == null) {
            LOGGER.debug("Blocking {}", x);
            invariantEncoding.ensureNoAccepting(x);
            return;
        }
        final FSA<S> possibleZ = FSAs.acceptingOnly(steadyAlphabet, process.postImage(y));
        if (possibleZ.acceptsNone()) {
            LOGGER.debug("Blocking {}", x);
            invariantEncoding.ensureNoAccepting(x);
            return;
        }

        final int takenX = solver.newFreeVariable();
        invariantEncoding.ensureAcceptingIfOnlyIf(takenX, x);
        final int certainZExists = solver.newFreeVariable();
        final CertainWord<S> z = invariantEncoding.ensureAcceptingCertainWordIf(certainZExists, x.size());
        z.ensureAcceptedBy(possibleZ);
        final CertainWord<Pair<S, S>> xz = orderEncoding.ensureAcceptingCertainWordIf(certainZExists, x.size());
        final SetIterable<S> noEpsilonSteadyAlphabet = steadyAlphabet.noEpsilonSet();
        x.forEachWithIndex((chx, pos) -> noEpsilonSteadyAlphabet.forEach(chz -> {
            final Twin<S> chxz = Tuples.twin(chx, chz);
            final int chzAtPos = z.getCharacterIndicator(pos, chz);
            final int chxzAtPos = xz.getCharacterIndicator(pos, chxz);
            solver.addImplication(chzAtPos, chxzAtPos);
        }));

        solver.addImplication(takenX, certainZExists);
    }

    private void addLearnedConstraints(FSAEncoding<S> invariantEncoding, FSAEncoding<Pair<S, S>> orderEncoding,
        List<LanguageSubsetChecker.Counterexample<S>> l1KnownViolations,
        List<BehaviorEnclosureChecker.Counterexample<S>> l2KnownViolations,
        List<TransitivityChecker.Counterexample<S>> l3KnownViolations,
        List<AnySchedulerProgressivityChecker.Counterexample<S>> l4KnownViolations)
    {
        l1KnownViolations.forEach(v -> refineInitConfigsEncloser(invariantEncoding, v));
        l2KnownViolations.forEach(v -> refineBehaviorEncloser(solver, invariantEncoding, v));
        l3KnownViolations.forEach(v -> refineTransitivity(solver, orderEncoding, v));
        l4KnownViolations
            .forEach(v -> refineProgressivity(solver, invariantEncoding, orderEncoding, process, roundAlphabet, v));
    }

    @Override
    public void prove()
    {
        final AlphabetIntEncoder<S> invSymbolEncoding = AlphabetIntEncoders.create(wholeAlphabet);
        final AlphabetIntEncoder<Pair<S, S>> ordSymbolEncoding = AlphabetIntEncoders.create(orderAlphabet);

        // having empty string excluded makes searching from 0 or 1 meaningless
        invariantSizeBegin = invariantSizeBegin < 1 ? 1 : invariantSizeBegin;
        orderSizeBegin = orderSizeBegin < 2 ? 2 : orderSizeBegin;

        final List<LanguageSubsetChecker.Counterexample<S>> l1KnownViolations = new LinkedList<>();
        final List<BehaviorEnclosureChecker.Counterexample<S>> l2KnownViolations = new LinkedList<>();
        final List<TransitivityChecker.Counterexample<S>> l3KnownViolations = new LinkedList<>();
        final List<AnySchedulerProgressivityChecker.Counterexample<S>> l4KnownViolations = new LinkedList<>();

        search((invSize, ordSize) -> {
            LOGGER.info("Searching in state spaces {} & {} ..", invSize, ordSize);

            final FSAEncoding<S> invEnc = newFSAEncoding(solver, invSize, invSymbolEncoding, shapeInvariant);
            final FSAEncoding<Pair<S, S>> ordEnc = newFSAEncoding(solver, ordSize, ordSymbolEncoding, shapeOrder);
            ordEnc.ensureNoWordPurelyMadeOf(orderReflexiveSymbols);

            boolean contradiction = false;
            try {
                addLearnedConstraints(invEnc, ordEnc, l1KnownViolations, l2KnownViolations, l3KnownViolations,
                                      l4KnownViolations);
            } catch (ContradictionException e) {
                LOGGER.info("Trivial contradiction found");
                contradiction = true;
            }

            LanguageSubsetChecker.Result<S> l1;
            BehaviorEnclosureChecker.Result<S> l2 = null;
            TransitivityChecker.Result<S> l3;
            LanguageSubsetChecker.Result<S> l4Precheck;
            AnySchedulerProgressivityChecker.Counterexample<S> l4PrecheckViolation;
            AnySchedulerProgressivityChecker.Result<S> l4;
            while (!contradiction && solver.findItSatisfiable()) {
                contradiction = false;
                final FSA<S> invCand = invEnc.resolve();
                final FST<S, S> ordCand = FSTs.castFrom((MutableFSA<Pair<S, S>>) ordEnc.resolve());

                LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
                LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

                if ((l1 = checkInitConfigsEnclosure(initialConfigs, invCand)).rejected()) {
                    LOGGER.debug("Initial configurations enclosed: {}", l1);
                    l1KnownViolations.add(l1.counterexample());
                    refineInitConfigsEncloser(invEnc, l1.counterexample());
                }
                if (invEnclosesAll && (l2 = checkBehaviorEnclosure(allBehavior, invCand)).rejected()) {
                    LOGGER.debug("Transition behavior enclosed: {}", l2);
                    l2KnownViolations.add(l2.counterexample());
                    refineBehaviorEncloser(solver, invEnc, l2.counterexample());
                }
                if ((l3 = checkTransitivity(ordCand)).rejected()) {
                    LOGGER.debug("Strict pre-order relation: {}", l3);
                    l3KnownViolations.add(l3.counterexample());
                    refineTransitivity(solver, ordEnc, l3.counterexample());
                }
                if (!loosenInvariant && (l4Precheck = schedulerOperatesOnAllNonfinalInvariants(invCand)).rejected()) {
                    final ListIterable<S> violation = l4Precheck.counterexample().witness();
                    final ListIterable<Twin<S>> v = violation.collect(ch -> Tuples.twin(ch, null));
                    l4PrecheckViolation = new BasicAnySchedulerProgressivityChecker.Counterexample<>(v);
                    l4 = new BasicAnySchedulerProgressivityChecker.Result<>(false, l4PrecheckViolation);
                } else {
                    l4 = checkProgressivity(nonfinalScheduler, process, invCand, ordCand);
                }
                if (l4.rejected()) {
                    LOGGER.debug("Progressivity: {}", l4);
                    l4KnownViolations.add(l4.counterexample());
                    try {
                        refineProgressivity(solver, invEnc, ordEnc, process, roundAlphabet, l4.counterexample());
                    } catch (ContradictionException e) {
                        LOGGER.info("Trivial contradiction found");
                        contradiction = true;
                    }
                }

                LOGGER.info("Rules checked: {} {} {} {}", //
                            l1.passed(), invEnclosesAll ? l2.passed() : "--", l3.passed(), l4.passed());
                if (l1.passed() && (!invEnclosesAll || l2.passed()) && l3.passed() && l4.passed()) {
                    return Tuples.pair(invCand, ordCand);
                }
            }

            solver.reset();
            return null;
        });
    }

    @Override
    public void verify()
    {
        final FSA<S> invCand = givenInvariant.determinize().minimize();
        final FST<S, S> ordCand = givenOrder;

        final String l1 = checkInitConfigsEnclosure(initialConfigs, invCand).toString();
        final String l2 = invEnclosesAll ? checkBehaviorEnclosure(allBehavior, invCand).toString() : "--";
        final String l3 = checkTransitivity(ordCand).toString();
        final String l4 = checkProgressivity(nonfinalScheduler, process, invCand, ordCand).toString();

        LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
        LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

        System.out.println("Initial configurations enclosed: " + l1);
        System.out.println("All behavior enclosed: " + l2);
        System.out.println("Strict pre-order relation: " + l3);
        System.out.println("Progressivity: " + l4);
    }
}
