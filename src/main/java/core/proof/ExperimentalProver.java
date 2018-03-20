package core.proof;

import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fsa.MutableFSA;
import api.automata.fst.FST;
import api.automata.fst.FSTs;
import api.proof.*;
import common.sat.ContradictionException;
import common.sat.SatSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.LinkedList;
import java.util.List;

import static common.util.Constants.DISPLAY_NEWLINE;
import static core.proof.CAV16MonoProver.*;

public class ExperimentalProver<S> extends AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final FairnessProgressivityChecker FAIRNESS_PROGRESSIVITY_CHECKER;

    private final FST<S, S> allBehavior;
    private final FSA<S> matteringConfigs;

    static {
        FAIRNESS_PROGRESSIVITY_CHECKER = new BasicFairnessProgressivityChecker();
    }

    public ExperimentalProver(Problem<S> problem, boolean shapeInvariant, boolean shapeOrder, boolean loosenInvariant)
    {
        super(problem, shapeInvariant, shapeOrder, loosenInvariant);

        allBehavior = scheduler.compose(process, scheduler.alphabet());
        final FSA<S> allBehaviorDomain = allBehavior.domain();
        matteringConfigs = allBehaviorDomain.intersect(nonfinalConfigs);
        LOGGER.debug("All behaviour computed: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", allBehavior);
    }

    private static <S> FairnessProgressivityChecker.Result<S> checkProgressivity(FST<S, S> behavior,
        FSA<S> matteringConfigs, FSA<S> invariant, FST<S, S> order)
    {
        return FAIRNESS_PROGRESSIVITY_CHECKER.test(behavior, matteringConfigs, invariant, order);
    }

    private static <S> void refineProgressivity(SatSolver solver, FSAEncoding<S> invariantEncoding,
        FSAEncoding<Pair<S, S>> orderEncoding, FairnessProgressivityChecker.Counterexample<S> counterexample)
    {
        final ListIterable<S> x = counterexample.fruitlessStep();
        final RichIterable<ListIterable<S>> possibleY = counterexample.possibleProgressSteps();
        if (possibleY.isEmpty()) {
            LOGGER.debug("Blocking {}", x);
            invariantEncoding.ensureNoAccepting(x);
            return;
        }

        final int takenX = solver.newFreeVariable();
        invariantEncoding.ensureAcceptingIfOnlyIf(takenX, x);
        final IntIterable takenAtLeastOneXY = possibleY.collectInt(y -> {
            final int takenXY = solver.newFreeVariable();
            orderEncoding.ensureAcceptingIfOnlyIf(takenXY, Alphabets.pairWord(x, y));
            return takenXY;
        });
        solver.addClauseIf(takenX, takenAtLeastOneXY.toArray());
    }

    private void addLearnedConstraints(FSAEncoding<S> invEnc, FSAEncoding<Pair<S, S>> ordEnc,
        List<LanguageSubsetChecker.Counterexample<S>> l1KnownViolations,
        List<BehaviorEnclosureChecker.Counterexample<S>> l2KnownViolations,
        List<TransitivityChecker.Counterexample<S>> l3KnownViolations,
        List<FairnessProgressivityChecker.Counterexample<S>> l4KnownViolations)
    {
        LOGGER.debug("Adding learned constraints: {}, {}, {}, {} ..", //
                     l1KnownViolations::size, l2KnownViolations::size, l3KnownViolations::size,
                     l4KnownViolations::size);

        l1KnownViolations.forEach(violation -> refineInitConfigsEncloser(invEnc, violation));
        l2KnownViolations.forEach(violation -> refineBehaviorEncloser(solver, invEnc, violation));
        l3KnownViolations.forEach(violation -> refineTransitivity(solver, ordEnc, violation));
        l4KnownViolations.forEach(violation -> refineProgressivity(solver, invEnc, ordEnc, violation));
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
        final List<FairnessProgressivityChecker.Counterexample<S>> l4KnownViolations = new LinkedList<>();

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
                LOGGER.info("Trivial contradiction found when applying learned constraints.");
                contradiction = true;
            }

            LanguageSubsetChecker.Result<S> l1;
            BehaviorEnclosureChecker.Result<S> l2;
            TransitivityChecker.Result<S> l3;
            LanguageSubsetChecker.Result<S> l4Precheck;
            FairnessProgressivityChecker.Counterexample<S> l4PrecheckViolation;
            FairnessProgressivityChecker.Result<S> l4;
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
                    final ListIterable<S> v = l4Precheck.counterexample().witness();
                    l4PrecheckViolation = new BasicFairnessProgressivityChecker.Counterexample<>(allBehavior, v);
                    l4 = new BasicFairnessProgressivityChecker.Result<>(false, l4PrecheckViolation);
                } else {
                    l4 = checkProgressivity(allBehavior, matteringConfigs, invCand, ordCand);
                }
                if (l4.rejected()) {
                    LOGGER.debug("Progressivity: {}", l4);
                    l4KnownViolations.add(l4.counterexample());
                    try {
                        refineProgressivity(solver, invEnc, ordEnc, l4.counterexample());
                    } catch (ContradictionException e) {
                        LOGGER.info("Trivial contradiction found when applying CE4.");
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
        final FSA<S> invCand = givenInvariant.determinize().minimize();
        final FST<S, S> ordCand = givenOrder;

        final String l1 = checkInitConfigsEnclosure(initialConfigs, invCand).toString();
        final String l2 = checkBehaviorEnclosure(allBehavior, invCand).toString();
        final String l3 = checkTransitivity(ordCand).toString();
        final String l4 = checkProgressivity(allBehavior, nonfinalConfigs, invCand, ordCand).toString();

        LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
        LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

        System.out.println("Initial configurations enclosed: " + l1);
        System.out.println("All behavior enclosed: " + l2);
        System.out.println("Strict pre-order relation: " + l3);
        System.out.println("Progressivity: " + l4);
    }
}
