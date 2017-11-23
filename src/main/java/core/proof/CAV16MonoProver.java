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
import static api.util.Values.DISPLAY_INDENT;

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
        allBehavior = FSAs.determinize(FSAs.union(scheduler, process));
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
                                          AnySchedulerProgressabilityChecker.Counterexample<S> counterexample)
    {
        final ImmutableList<S> x = counterexample.get().collect(Twin::getOne);
        final ImmutableList<S> y = counterexample.get().collect(Twin::getTwo);

        final int takenX = solver.newFreeVariable();
        invariantEncoding.ensureAcceptingIfOnlyIf(takenX, x);

        final int shouldBeCertainZ = solver.newFreeVariable();
        final CertainWord<S> z = invariantEncoding.ensureAcceptingCertainWordIf(shouldBeCertainZ, x.size());
        final FSA<S> possibleZ = FSAs.thatAcceptsOnly(z.alphabet(), Transducers.postImage(process, y));
        z.ensureAcceptedBy(FSAs.determinize(possibleZ));
        final CertainWord<Twin<S>> xz = orderEncoding.ensureAcceptingCertainWordIf(shouldBeCertainZ, x.size());
        x.forEachWithIndex((chx, pos) -> {
            z.alphabet().noEpsilonSet().forEach(chz -> {
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
        final Alphabet<S> alphabet = initialConfigs.alphabet();
        final AlphabetIntEncoder<S> invSymbolEncoding = AlphabetIntEncoders.create(alphabet);
        final AlphabetIntEncoder<Twin<S>> ordSymbolEncoding = AlphabetIntEncoders.create(scheduler.alphabet());
        final ImmutableSet<Twin<S>> reflexiveRelSymbols = alphabet.noEpsilonSet().collect(s -> Tuples.twin(s, s));

        // having empty string excluded makes searching from 0 or 1 meaningless
        invariantSizeBegin = invariantSizeBegin < 2 ? 2 : invariantSizeBegin;
        orderSizeBegin = orderSizeBegin < 2 ? 2 : orderSizeBegin;

        search((invSize, ordSize) -> {
            LOGGER.info("Searching in state spaces {} & {} ..", invSize, ordSize);

            final FSAEncoding<S> invGuessing = newFSAEncoding(solver, invSize, invSymbolEncoding);
            final FSAEncoding<Twin<S>> ordGuessing = newFSAEncoding(solver, ordSize, ordSymbolEncoding);
            ordGuessing.ensureNoWordPurelyMadeOf(reflexiveRelSymbols);

            LanguageSubsetChecker.Result<S> l1;
            BehaviorEnclosureChecker.Result<S> l2 = null;
            TransitivityChecker.Result<S> l3;
            AnySchedulerProgressabilityChecker.Result<S> l4;
            while (solver.findItSatisfiable()) {
                final FSA<S> invCand = invGuessing.resolve();
                final FSA<Twin<S>> ordCand = ordGuessing.resolve();

                if ((l1 = checkInitConfigsEnclosure(initialConfigs, invCand)).rejected()) {
                    refineInitConfigsEncloser(invGuessing, l1.counterexample());
                }
                if (invEnclosesAll && (l2 = checkBehaviorEnclosure(allBehavior, invCand)).rejected()) {
                    refineBehaviorEncloser(solver, invGuessing, l2.counterexample());
                }
                if ((l3 = checkTransitivity(ordCand)).rejected()) {
                    refineTransitivity(solver, ordGuessing, l3.counterexample());
                }
                if ((l4 = checkProgressability(nfScheduler, process, nonfinalConfigs, invCand, ordCand)).rejected()) {
                    refineProgressability(solver, invGuessing, ordGuessing, process, l4.counterexample());
                }

                LOGGER.info("Having counterexamples: {} {} {} {}", //
                            l1.passed(), (!invEnclosesAll || l2.passed()), l3.passed(), l4.passed());
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
        final String indent = DISPLAY_INDENT + "-- ";
        final FSA<S> invCand = FSAs.determinize(givenInvariant);
        final FSA<Twin<S>> ordCand = FSAs.determinize(givenOrder);

        System.out.println("Initial Configurations Enclosed");
        System.out.println(indent + checkInitConfigsEnclosure(initialConfigs, invCand));
        System.out.println("Transition Behavior Enclosed");
        System.out.println(indent + checkBehaviorEnclosure(allBehavior, invCand));
        System.out.println("Strict Pre-order Relation");
        System.out.println(indent + checkTransitivity(ordCand));
        System.out.println("Fairness Progressability");
        System.out.println(indent + checkProgressability(nfScheduler, process, nonfinalConfigs, invCand, ordCand));
    }
}
