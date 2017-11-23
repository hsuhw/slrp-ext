package core.proof;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.util.Values.DISPLAY_INDENT;
import static core.proof.CAV16MonoProver.*;

public class ExperimentalProver<S> extends AbstractProver<S> implements Prover
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final FairnessProgressabilityChecker FAIRNESS_PROGRESSABILITY_CHECKER;

    private final FSA<Twin<S>> allBehavior;

    static {
        FAIRNESS_PROGRESSABILITY_CHECKER = new BasicFairnessProgressabilityChecker();
    }

    public ExperimentalProver(Problem<S> problem)
    {
        super(problem);

        allBehavior = FSAs.determinize(Transducers.compose(scheduler, process, scheduler.alphabet()));
    }

    static <S> FairnessProgressabilityChecker.Result<S> checkProgressability(FSA<Twin<S>> behavior,
                                                                             FSA<S> nonfinalConfigs, FSA<S> invariant,
                                                                             FSA<Twin<S>> order)
    {
        return FAIRNESS_PROGRESSABILITY_CHECKER.test(behavior, nonfinalConfigs, invariant, order);
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
            BehaviorEnclosureChecker.Result<S> l2;
            TransitivityChecker.Result<S> l3;
            FairnessProgressabilityChecker.Result<S> l4;
            while (solver.findItSatisfiable()) {
                final FSA<S> invCand = invGuessing.resolve();
                final FSA<Twin<S>> ordCand = ordGuessing.resolve();

                if ((l1 = checkInitConfigsEnclosure(initialConfigs, invCand)).rejected()) {
                    refineInitConfigsEncloser(invGuessing, l1.counterexample());
                }
                if ((l2 = checkBehaviorEnclosure(allBehavior, invCand)).rejected()) {
                    refineBehaviorEncloser(solver, invGuessing, l2.counterexample());
                }
                if ((l3 = checkTransitivity(ordCand)).rejected()) {
                    refineTransitivity(solver, ordGuessing, l3.counterexample());
                }
                if ((l4 = checkProgressability(allBehavior, nonfinalConfigs, invCand, ordCand)).rejected()) {
                    refineProgressability(solver, invGuessing, ordGuessing, l4.counterexample());
                }

                LOGGER.info("Having counterexamples: {} {} {} {}", //
                            l1.passed(), l2.passed(), l3.passed(), l4.passed());
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
        System.out.println(indent + checkProgressability(allBehavior, nonfinalConfigs, invCand, ordCand));
    }
}
