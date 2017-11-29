package core.proof;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.AutomatonManipulator.selectFrom;
import static api.proof.FSAEncoding.CertainWord;
import static api.util.Connectives.AND;
import static api.util.Connectives.Labels;
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
        final FSA<Twin<S>> nfIn = FSAs
            .product(sched, nfConfigs, sched.alphabet(), Labels.whoseInputMatched(), (sm, builder) -> {
                builder.addStartStates(selectFrom(sm, sched::isStartState, AND, nfConfigs::isStartState));
                builder.addAcceptStates(selectFrom(sm, sched::isAcceptState, AND, nfConfigs::isAcceptState));
            });

        return FSAs.product(nfIn, nfConfigs, sched.alphabet(), Labels.whoseOutputMatched(), (sm, builder) -> {
            builder.addStartStates(selectFrom(sm, nfIn::isStartState, AND, nfConfigs::isStartState));
            builder.addAcceptStates(selectFrom(sm, nfIn::isAcceptState, AND, nfConfigs::isAcceptState));
        });
    }

    public CAV16MonoProver(Problem<S> problem)
    {
        super(problem);

        nfScheduler = makeNonfinalScheduler(scheduler, nonfinalConfigs);
        allBehavior = FSAs.minimize(FSAs.determinize(FSAs.union(scheduler, process)));
        invEnclosesAll = problem.invariantEnclosesAllBehavior();
    }

    static <S> FSAEncoding<S> newFSAEncoding(SatSolver solver, int size, AlphabetIntEncoder<S> alphabetEncoding)
    {
        FSAEncoding<S> instance = new BasicFSAEncoding<>(solver, size, alphabetEncoding);
        instance.ensureNoAccepting(Lists.immutable.empty());
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
                                                                                 FSA<Twin<S>> process, FSA<S> nfConfigs,
                                                                                 FSA<S> invariant, FSA<Twin<S>> order)
    {
        return ANY_SCHEDULER_PROGRESSABILITY_CHECKER.test(nfScheduler, process, nfConfigs, invariant, order);
    }

    static <S> void refineProgressability(SatSolver solver, FSAEncoding<S> invariantEncoding,
                                          FSAEncoding<Twin<S>> orderEncoding, FSA<Twin<S>> process,
                                          Alphabet<S> steadyAlphabet,
                                          AnySchedulerProgressabilityChecker.Counterexample<S> counterexample)
    {
        final ImmutableList<S> x = counterexample.get().collect(Twin::getOne);
        final ImmutableList<S> y = counterexample.get().collect(Twin::getTwo);

        final FSA<S> possibleZ = FSAs.thatAcceptsOnly(steadyAlphabet, Transducers.postImage(process, y));
        if (possibleZ.acceptsNone()) {
            invariantEncoding.ensureNoAccepting(x);
            return;
        }

        final int takenX = solver.newFreeVariable();
        invariantEncoding.ensureAcceptingIfOnlyIf(takenX, x);
        final int shouldBeCertainZ = solver.newFreeVariable();
        final CertainWord<S> z = invariantEncoding.ensureAcceptingCertainWordIf(shouldBeCertainZ, x.size());
        z.ensureAcceptedBy(FSAs.minimize(FSAs.determinize(possibleZ)));
        final CertainWord<Twin<S>> xz = orderEncoding.ensureAcceptingCertainWordIf(shouldBeCertainZ, x.size());
        x.forEachWithIndex((chx, pos) -> {
            steadyAlphabet.noEpsilonSet().forEach(chz -> {
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
        final AlphabetIntEncoder<S> invSymbolEncoding = AlphabetIntEncoders.create(allAlphabet);
        final AlphabetIntEncoder<Twin<S>> ordSymbolEncoding = AlphabetIntEncoders.create(orderAlphabet);
        final ImmutableSet<Twin<S>> ordReflexiveSymbols = steadyAlphabet.asSet().collect(s -> Tuples.twin(s, s));

        // having empty string excluded makes searching from 0 or 1 meaningless
        invariantSizeBegin = invariantSizeBegin < 2 ? 2 : invariantSizeBegin;
        orderSizeBegin = orderSizeBegin < 2 ? 2 : orderSizeBegin;

        search((invSize, ordSize) -> {
            LOGGER.info("Searching in state spaces {} & {} ..", invSize, ordSize);

            final FSAEncoding<S> inv = newFSAEncoding(solver, invSize, invSymbolEncoding);
            final FSAEncoding<Twin<S>> ord = newFSAEncoding(solver, ordSize, ordSymbolEncoding);
            ord.ensureNoWordPurelyMadeOf(ordReflexiveSymbols);

            LanguageSubsetChecker.Result<S> l1;
            BehaviorEnclosureChecker.Result<S> l2 = null;
            TransitivityChecker.Result<S> l3;
            AnySchedulerProgressabilityChecker.Result<S> l4;
            while (solver.findItSatisfiable()) {
                final FSA<S> invCand = inv.resolve();
                final FSA<Twin<S>> ordCand = ord.resolve();

                if ((l1 = checkInitConfigsEnclosure(initialConfigs, invCand)).rejected()) {
                    refineInitConfigsEncloser(inv, l1.counterexample());
                }
                if (invEnclosesAll && (l2 = checkBehaviorEnclosure(allBehavior, invCand)).rejected()) {
                    refineBehaviorEncloser(solver, inv, l2.counterexample());
                }
                if ((l3 = checkTransitivity(ordCand)).rejected()) {
                    refineTransitivity(solver, ord, l3.counterexample());
                }
                if ((l4 = checkProgressability(nfScheduler, process, nonfinalConfigs, invCand, ordCand)).rejected()) {
                    refineProgressability(solver, inv, ord, process, steadyAlphabet, l4.counterexample());
                }

                LOGGER.info("Having counterexamples: {} {} {} {}", //
                            l1.passed(), invEnclosesAll ? l2.passed() : "--", l3.passed(), l4.passed());
                if (l1.passed() && (!invEnclosesAll || l2.passed()) && l3.passed() && l4.passed()) {
                    return Tuples.pair(invCand, ordCand);
                } else {
                    LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
                    LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);
                    LOGGER.debug("Initial configurations enclosed: {}", l1);
                    LOGGER.debug("Transition behavior enclosed: {}", invEnclosesAll ? l2 : "--");
                    LOGGER.debug("Strict pre-order relation: {}", l3);
                    LOGGER.debug("Progressability: {}", l4);
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
        final String l4 = checkProgressability(nfScheduler, process, nonfinalConfigs, invCand, ordCand).toString();

        LOGGER.debug("Invariant candidate: " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", invCand);
        LOGGER.debug("Order candidate (>): " + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", ordCand);

        System.out.println("Initial configurations enclosed: " + l1);
        System.out.println("All behavior enclosed: " + l2);
        System.out.println("Strict pre-order relation: " + l3);
        System.out.println("Progressability: " + l4);
    }
}
