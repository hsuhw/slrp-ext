package core.proof;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LanguageSubsetChecker;
import api.proof.FSAEncoding;
import api.proof.SatSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.util.Connectives.AND;

public class Prover
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final FSA<String> initialConfigs;
    private final FSA<String> nonfinalConfigs;
    private final FSA<String> invariantCand;
    private final Alphabet<Twin<String>> relationAlphabet;
    private final FSA<Twin<String>> transBehavior;
    private final FSA<Twin<String>> relationCand;
    private final int invariantSizeBound;
    private final int invariantSizeBegin;
    private final int relationSizeBound;
    private final int relationSizeBegin;
    private final SatSolver solver;

    static FSA<Twin<String>> composeTransducer(FSA<Twin<String>> one, FSA<Twin<String>> two,
                                               Alphabet<Twin<String>> productAlphabet)
    {
        return FSAs.product(one, two, productAlphabet, (s1, s2) -> {
            return s1.getTwo().equals(s2.getOne()) ? Tuples.twin(s1.getOne(), s2.getTwo()) : null;
        }, (stateMapping, builder) -> {
            final ImmutableSet<State> startStates = AutomatonManipulator
                .selectFromProduct(stateMapping, one::isStartState, two::isStartState, AND);
            final ImmutableSet<State> acceptStates = AutomatonManipulator
                .selectFromProduct(stateMapping, one::isAcceptState, two::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });
    }

    public Prover(Problem problem)
    {
        initialConfigs = FSAs.determinize(problem.initialConfigurations());
        nonfinalConfigs = FSAs.complement(problem.finalConfigurations());
        invariantCand = problem.invariant();
        relationCand = problem.orderRelation();

        relationAlphabet = Alphabets.product(initialConfigs.alphabet());
        final FSA<Twin<String>> scheduler = problem.schedulerBehavior();
        final FSA<Twin<String>> process = problem.processBehavior();
        transBehavior = FSAs.determinize(composeTransducer(scheduler, process, relationAlphabet));

        final IntIntPair invSearchBound = problem.invariantSizeBound();
        final IntIntPair relSearchBound = problem.orderRelationSizeBound();
        invariantSizeBegin = invSearchBound.getOne();
        invariantSizeBound = invSearchBound.getTwo();
        relationSizeBegin = relSearchBound.getOne();
        relationSizeBound = relSearchBound.getTwo();
        solver = new Sat4jSolverAdapter();
    }

    static ImmutableList<String> verifyL1(FSA<String> initConfigs, FSA<String> invCand)
    {
        final LanguageSubsetChecker.Result<String> invCandEnclosesInit = FSAs.checkSubset(initConfigs, invCand);

        return invCandEnclosesInit.rejected() ? invCandEnclosesInit.counterexample().get() : null;
    }

    static void applyCE1(FSAEncoding<String> invGuessing, ImmutableList<String> counterexample)
    {
        invGuessing.ensureAcceptingWord(counterexample);
    }

    static MutableSet<MutableList<String>> computeReversedPostImage(FSA<Twin<String>> target, State state,
                                                                    ImmutableList<String> word, boolean opposite,
                                                                    int capacity)
    {
        if (word.size() == 0) {
            final MutableSet<MutableList<String>> blankSingleton = UnifiedSet.newSet(1);
            blankSingleton.add(FastList.newList(capacity));
            return target.isAcceptState(state) ? blankSingleton : Sets.mutable.empty();
        }

        final TransitionGraph<State, Twin<String>> delta = target.transitionGraph();
        final int resultSizeBound = target.alphabet().size() * target.states().size();
        final MutableSet<MutableList<String>> result = UnifiedSet.newSet(resultSizeBound); // upper bound

        String symbol1, symbol2, input, output;
        SetIterable<MutableList<String>> recursion;
        for (Twin<String> ioTrans : delta.enabledArcsOn(state)) {
            symbol1 = ioTrans.getOne();
            symbol2 = ioTrans.getTwo();
            input = opposite ? symbol2 : symbol1;
            output = opposite ? symbol1 : symbol2;
            if (input.equals(word.get(0))) {
                for (State dest : delta.successorsOf(state, ioTrans)) {
                    recursion = computeReversedPostImage(target, dest, word.subList(1, word.size()), opposite,
                                                         capacity);
                    for (MutableList<String> postfix : recursion) {
                        postfix.add(output);
                        result.add(postfix);
                    }
                }
            }
        }

        return result;
    }

    static <S> ImmutableList<S> reversedAndImmutable(MutableList<S> word)
    {
        return word.reverseThis().toImmutable();
    }

    static ImmutableSet<ImmutableList<String>> computePostImage(FSA<Twin<String>> target, ImmutableList<String> word)
    {
        final State startState = target.startState();

        return computeReversedPostImage(target, startState, word, false, word.size())
            .collect(Prover::reversedAndImmutable).toImmutable();
    }

    static ImmutableSet<ImmutableList<String>> computePreImage(FSA<Twin<String>> target, ImmutableList<String> word)
    {
        final State startState = target.startState();

        return computeReversedPostImage(target, startState, word, true, word.size())
            .collect(Prover::reversedAndImmutable).toImmutable();
    }

    static Pair<ImmutableSet<ImmutableList<String>>, ImmutableList<String>> verifyL2(FSA<Twin<String>> transBehavior,
                                                                                     FSA<String> invCand)
    {
        final FSA<String> postImage = FSAs.product(invCand, transBehavior, invCand.alphabet(), (s1, s2) -> {
            return s1.equals(s2.getOne()) ? s2.getTwo() : null;
        }, (stateMapping, builder) -> {
            final ImmutableSet<State> startStates = AutomatonManipulator
                .selectFromProduct(stateMapping, invCand::isStartState, transBehavior::isStartState, AND);
            final ImmutableSet<State> acceptStates = AutomatonManipulator
                .selectFromProduct(stateMapping, invCand::isAcceptState, transBehavior::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });

        final LanguageSubsetChecker.Result<String> invCandEnclosesTrans = FSAs.checkSubset(postImage, invCand);

        if (invCandEnclosesTrans.passed()) {
            return null;
        }
        final ImmutableList<String> witness = invCandEnclosesTrans.counterexample().get();

        return Tuples.pair(computePreImage(transBehavior, witness), witness);
    }

    static void applyCE2(SatSolver solver, FSAEncoding<String> invGuessing,
                         Pair<ImmutableSet<ImmutableList<String>>, ImmutableList<String>> counterexample)
    {
        final ImmutableList<String> y = counterexample.getTwo();
        final int takenY = solver.newFreeVariables(1).getFirst();
        invGuessing.whetherAcceptWord(takenY, y);
        for (ImmutableList<String> x : counterexample.getOne()) {
            final int takenX = solver.newFreeVariables(1).getFirst();
            invGuessing.whetherAcceptWord(takenX, x);
            solver.addImplication(takenX, takenY);
        }
    }

    static Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> verifyL3(FSA<Twin<String>> relCand)
    {
        final FSA<Twin<String>> transitive = FSAs.product(relCand, relCand, relCand.alphabet(), (s1, s2) -> {
            return s1.getTwo().equals(s2.getOne()) ? Tuples.twin(s1.getOne(), s2.getTwo()) : null;
        }, (stateMapping, builder) -> {
            final ImmutableSet<State> startStates = AutomatonManipulator
                .selectFromProduct(stateMapping, relCand::isStartState, relCand::isStartState, AND);
            final ImmutableSet<State> acceptStates = AutomatonManipulator
                .selectFromProduct(stateMapping, relCand::isAcceptState, relCand::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });

        final LanguageSubsetChecker.Result<Twin<String>> relCandBeTransitive = FSAs.checkSubset(transitive, relCand);
        if (relCandBeTransitive.passed()) {
            return null;
        }
        final ImmutableList<Twin<String>> witness = relCandBeTransitive.counterexample().get();
        final ImmutableList<String> x = witness.collect(Twin::getOne);
        final ImmutableList<String> z = witness.collect(Twin::getTwo);
        final ImmutableSet<ImmutableList<String>> xPostImage = computePostImage(relCand, x);
        final ImmutableSet<ImmutableList<String>> zPreImage = computePreImage(relCand, z);
        return Tuples.pair(witness, Sets.intersect(xPostImage.castToSet(), zPreImage.castToSet()));
    }

    static void applyCE3(SatSolver solver, FSAEncoding<Twin<String>> relGuessing,
                         Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> counterexample)
    {
        final ImmutableList<String> x = counterexample.getOne().collect(Twin::getOne);
        final ImmutableList<String> z = counterexample.getOne().collect(Twin::getTwo);
        final int takenXZ = solver.newFreeVariables(1).getFirst();
        relGuessing.whetherAcceptWord(takenXZ, counterexample.getOne());
        for (ImmutableList<String> y : counterexample.getTwo()) {
            final int takenXY = solver.newFreeVariables(1).getFirst();
            final int takenYZ = solver.newFreeVariables(1).getFirst();

            relGuessing.whetherAcceptWord(takenXY, Alphabets.twinWord(x, y));
            relGuessing.whetherAcceptWord(takenYZ, Alphabets.twinWord(y, z));
            solver.addClause(-takenXY, -takenYZ, takenXZ);
        }
    }

    static Pair<ImmutableList<String>, ImmutableSet<ImmutableList<String>>> verifyL4(FSA<Twin<String>> transBehavior,
                                                                                     FSA<String> nonfinalConfigs,
                                                                                     FSA<String> invCand,
                                                                                     FSA<Twin<String>> relCand)
    {
        final FSA<Twin<String>> havingSmallerStep = FSAs.intersect(transBehavior, relCand);
        final FSA<String> smallerAvail = FSAs.project(havingSmallerStep, invCand.alphabet(), Twin::getOne);
        System.out.println(smallerAvail);
        final FSA<String> nonfinalInv = FSAs.intersect(invCand, nonfinalConfigs);
        final LanguageSubsetChecker.Result<String> nonfinalSmallerAvail = FSAs.checkSubset(nonfinalInv, smallerAvail);
        if (nonfinalSmallerAvail.passed()) {
            return null;
        }
        final ImmutableList<String> witness = nonfinalSmallerAvail.counterexample().get();
        return Tuples.pair(witness, computePostImage(transBehavior, witness));
    }

    static void applyCE4(SatSolver solver, FSAEncoding<String> invGuessing, FSAEncoding<Twin<String>> relGuessing,
                         Pair<ImmutableList<String>, ImmutableSet<ImmutableList<String>>> counterexample)
    {
        final ImmutableList<String> x = counterexample.getOne();
        final int takenX = solver.newFreeVariables(1).getFirst();
        invGuessing.whetherAcceptWord(takenX, x);
        final ImmutableSet<ImmutableList<String>> possibleY = counterexample.getTwo();
        final ImmutableIntList takenAtLeastOneXY = possibleY.toList().collectInt(y -> {
            final int takenXY = solver.newFreeVariables(1).getFirst();
            relGuessing.whetherAcceptWord(takenXY, Alphabets.twinWord(x, y));
            return takenXY;
        }).toImmutable();
        solver.addClauseIf(takenX, takenAtLeastOneXY);
    }

    public void prove()
    {
        final Alphabet<String> alphabet = initialConfigs.alphabet();
        final AlphabetIntEncoder<String> invSymbolEncoding = AlphabetIntEncoders.create(alphabet);
        final AlphabetIntEncoder<Twin<String>> relSymbolEncoding = AlphabetIntEncoders.create(relationAlphabet);
        final ImmutableSet<Twin<String>> reflexiveRelSymbols = alphabet.noEpsilonSet().collect(s -> Tuples.twin(s, s));

        FSAEncoding<String> invGuessing;
        FSAEncoding<Twin<String>> relGuessing;
        FSA<String> invCand;
        FSA<Twin<String>> relCand;
        ImmutableList<String> c1;
        Pair<ImmutableSet<ImmutableList<String>>, ImmutableList<String>> c2;
        Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> c3;
        Pair<ImmutableList<String>, ImmutableSet<ImmutableList<String>>> c4;
        final int stabilizerBound = invariantSizeBound * invariantSizeBound + relationSizeBound * relationSizeBound;
        for (int stabilizer = 1; stabilizer <= stabilizerBound; stabilizer++) {
            for (int invSearching = invariantSizeBegin; invSearching <= invariantSizeBound; invSearching++) {
                for (int relSearching = relationSizeBegin; relSearching <= relationSizeBound; relSearching++) {
                    final int stabilizerFactor = invSearching * invSearching + relSearching * relSearching;
                    if (stabilizerFactor != stabilizer) {
                        continue;
                    }

                    LOGGER.info("Searching in state spaces {} & {} ..", invSearching, relSearching);
                    invGuessing = new BasicFSAEncoding<>(solver, invSearching, invSymbolEncoding);
                    invGuessing.ensureNoAcceptingWord(Lists.immutable.empty());
                    invGuessing.ensureNoDanglingState();
                    relGuessing = new BasicFSAEncoding<>(solver, relSearching, relSymbolEncoding);
                    relGuessing.ensureNoAcceptingWord(Lists.immutable.empty());
                    relGuessing.ensureNoDanglingState();
                    relGuessing.ensureNoWordPurelyMadeOf(reflexiveRelSymbols);

                    while (solver.findItSatisfiable()) {
                        invCand = invGuessing.resolve();
                        relCand = relGuessing.resolve();

                        if ((c1 = verifyL1(initialConfigs, invCand)) != null) {
                            applyCE1(invGuessing, c1);
                        }
                        if ((c2 = verifyL2(transBehavior, invCand)) != null) {
                            applyCE2(solver, invGuessing, c2);
                        }
                        if ((c3 = verifyL3(relCand)) != null) {
                            applyCE3(solver, relGuessing, c3);
                        }
                        if ((c4 = verifyL4(transBehavior, nonfinalConfigs, invCand, relCand)) != null) {
                            applyCE4(solver, invGuessing, relGuessing, c4);
                        }

                        LOGGER.warn("Counterexample: {} {} {} {}", //
                                    c1 == null, c2 == null, c3 == null, c4 == null);
//                        if (c1 == null && c2 == null) {
//                            System.out.println(invCand);
//                            return;
//                        }
                        if (c1 == null && c2 == null && c3 == null && c4 == null) {
                            System.out.println("Proof found under the given search bound.");
                            System.out.println("A " + invCand);
                            System.out.println("T " + relCand);
                            return;
                        }
                    }

                    solver.reset();
                }
            }
        }

        System.out.println("No proof found under the given search bound.");
    }

    public void verify()
    {
        final FSA<String> invCand = FSAs.determinize(invariantCand);
        FSA<Twin<String>> relCand = FSAs.determinize(relationCand);

        final ImmutableList<String> c1 = verifyL1(initialConfigs, invCand);
        final Pair<ImmutableSet<ImmutableList<String>>, ImmutableList<String>> c2 = verifyL2(transBehavior, invCand);
        final Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> c3 = verifyL3(relCand);
        final Pair<ImmutableList<String>, ImmutableSet<ImmutableList<String>>> c4 = verifyL4(transBehavior,
                                                                                             nonfinalConfigs, invCand,
                                                                                             relCand);

        System.out.println(c1);
        System.out.println(c2);
        System.out.println(c3);
        System.out.println(c4);
    }
}
