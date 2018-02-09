package core.proof;

import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.Alphabets;
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
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.List;

import static api.util.Values.DISPLAY_NEWLINE;
import static core.proof.CAV16MonoProver.*;

public class ExperimentalProver<S> extends AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final FairnessProgressabilityChecker FAIRNESS_PROGRESSABILITY_CHECKER;

    private final FSA<Twin<S>> allBehavior;
    private final FSA<S> matteringConfigs;

    static {
        FAIRNESS_PROGRESSABILITY_CHECKER = new BasicFairnessProgressabilityChecker();
    }

    public ExperimentalProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder, boolean loosenInvariant)
    {
        super(problem, shapeInvariant, shapeOrder, loosenInvariant);

        allBehavior = Transducers.compose(scheduler, process, scheduler.alphabet());
        final FSA<S> allBehaviorDomain = FSAs.project(allBehavior, wholeAlphabet, Twin::getOne);
        matteringConfigs = FSAs.intersect(nonfinalConfigs, allBehaviorDomain);
        LOGGER.debug("All behaviour computed: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", allBehavior);
    }

    static <S> FairnessProgressabilityChecker.Result<S> checkProgressability(FSA<Twin<S>> behavior,
                                                                             FSA<S> matteringConfigs, FSA<S> invariant,
                                                                             FSA<Twin<S>> order)
    {
        return FAIRNESS_PROGRESSABILITY_CHECKER.test(behavior, matteringConfigs, invariant, order);
    }

    static <S> void refineProgressability(SatSolver solver, FSAEncoding<S> invariantEncoding,
                                          FSAEncoding<Twin<S>> orderEncoding,
                                          FairnessProgressabilityChecker.Counterexample<S> counterexample)
    {
        final ImmutableList<S> x = counterexample.get();
        final int takenX = solver.newFreeVariable();
        invariantEncoding.ensureAcceptingIfOnlyIf(takenX, x);
        final ImmutableSet<ImmutableList<S>> ys = counterexample.causes();
        final ImmutableIntSet takenAtLeastOneXY = ys.collectInt(y -> {
            final int takenXY = solver.newFreeVariable();
            orderEncoding.ensureAcceptingIfOnlyIf(takenXY, Alphabets.twinWord(x, y));
            return takenXY;
        });
        solver.addClauseIf(takenX, takenAtLeastOneXY);
    }

    private void addLearnedConstraints(FSAEncoding<S> invEnc, FSAEncoding<Twin<S>> ordEnc,
                                       List<LanguageSubsetChecker.Counterexample<S>> l1KnownViolations,
                                       List<BehaviorEnclosureChecker.Counterexample<S>> l2KnownViolations,
                                       List<TransitivityChecker.Counterexample<S>> l3KnownViolations,
                                       List<FairnessProgressabilityChecker.Counterexample<S>> l4KnownViolations)
    {
        l1KnownViolations.forEach(violation -> refineInitConfigsEncloser(invEnc, violation));
        l2KnownViolations.forEach(violation -> refineBehaviorEncloser(solver, invEnc, violation));
        l3KnownViolations.forEach(violation -> refineTransitivity(solver, ordEnc, violation));
        l4KnownViolations.forEach(violation -> refineProgressability(solver, invEnc, ordEnc, violation));
    }

    @Override
    public void prove()
    {
        final AlphabetIntEncoder<S> invSymbolEncoding = AlphabetIntEncoders.create(wholeAlphabet);
        final AlphabetIntEncoder<Twin<S>> ordSymbolEncoding = AlphabetIntEncoders.create(orderAlphabet);

        // having empty string excluded makes searching from 0 or 1 meaningless
        invariantSizeBegin = invariantSizeBegin < 1 ? 1 : invariantSizeBegin;
        orderSizeBegin = orderSizeBegin < 2 ? 2 : orderSizeBegin;

        final List<LanguageSubsetChecker.Counterexample<S>> l1KnownViolations = new LinkedList<>();
        final List<BehaviorEnclosureChecker.Counterexample<S>> l2KnownViolations = new LinkedList<>();
        final List<TransitivityChecker.Counterexample<S>> l3KnownViolations = new LinkedList<>();
        final List<FairnessProgressabilityChecker.Counterexample<S>> l4KnownViolations = new LinkedList<>();

        search((invSize, ordSize) -> {
            LOGGER.info("Searching in state spaces {} & {} ..", invSize, ordSize);

            final FSAEncoding<S> invEnc = newFSAEncoding(solver, invSize, invSymbolEncoding, shapeInvariant);
            final FSAEncoding<Twin<S>> ordEnc = newFSAEncoding(solver, ordSize, ordSymbolEncoding, shapeOrder);
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
            BehaviorEnclosureChecker.Result<S> l2;
            TransitivityChecker.Result<S> l3;
            LanguageSubsetChecker.Result<S> l4Precheck;
            FairnessProgressabilityChecker.Counterexample<S> l4PrecheckViolation;
            FairnessProgressabilityChecker.Result<S> l4;
            while (!contradiction && solver.findItSatisfiable()) {
                contradiction = false;
                final FSA<S> invCand = invEnc.resolve();
                final FSA<Twin<S>> ordCand = ordEnc.resolve();

                LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
                LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

                if ((l1 = checkInitConfigsEnclosure(initialConfigs, invCand)).rejected()) {
                    LOGGER.debug("Initial configurations enclosed: {}", l1);
                    l1KnownViolations.add(l1.counterexample());
                    refineInitConfigsEncloser(invEnc, l1.counterexample());
                }
                if ((l2 = checkBehaviorEnclosure(allBehavior, invCand)).rejected()) {
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
                    final ImmutableList<S> v = l4Precheck.counterexample().get();
                    l4PrecheckViolation = new BasicFairnessProgressabilityChecker.Counterexample<>(allBehavior, v);
                    l4 = new BasicFairnessProgressabilityChecker.Result<>(false, l4PrecheckViolation);
                } else {
                    l4 = checkProgressability(allBehavior, matteringConfigs, invCand, ordCand);
                }
                if (l4.rejected()) {
                    LOGGER.debug("Progressability: {}", l4);
                    l4KnownViolations.add(l4.counterexample());
                    try {
                        refineProgressability(solver, invEnc, ordEnc, l4.counterexample());
                    } catch (ContradictionException e) {
                        LOGGER.info("Trivial contradiction found");
                        contradiction = true;
                    }
                }

                LOGGER.info("Rules checked: {} {} {} {}", l1.passed(), l2.passed(), l3.passed(), l4.passed());
                if (l1.passed() && l2.passed() && l3.passed() && l4.passed()) {
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
        final FSA<S> invCand = FSAs.determinize(givenInvariant);
        final FSA<Twin<S>> ordCand = FSAs.determinize(givenOrder);

        final String l1 = checkInitConfigsEnclosure(initialConfigs, invCand).toString();
        final String l2 = checkBehaviorEnclosure(allBehavior, invCand).toString();
        final String l3 = checkTransitivity(ordCand).toString();
        final String l4 = checkProgressability(allBehavior, nonfinalConfigs, invCand, ordCand).toString();

        LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
        LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

        System.out.println("Initial configurations enclosed: " + l1);
        System.out.println("All behavior enclosed: " + l2);
        System.out.println("Strict pre-order relation: " + l3);
        System.out.println("Progressability: " + l4);
    }
}
