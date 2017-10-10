package core;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAs;
import api.synth.FSAEncoding;
import api.synth.SatSolver;
import api.synth.SatSolverTimeoutException;
import core.synth.ReferenceFSAEncoding;
import core.synth.Sat4jSolverAdapter;
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
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.function.Function;

import static api.util.Connectives.AND;

public class Prover
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final FSA<String> initialConfigs;
    private final FSA<String> nonfinalConfigs;
    private final Alphabet<Twin<String>> relationAlphabet;
    private final FSA<Twin<String>> transBehavior;
    private final int invariantSizeBound;
    private final int invariantSizeBegin;
    private final int relationSizeBound;
    private final int relationSizeBegin;
    private final SatSolver solver;

    private static Alphabet<Twin<String>> makeCombinationAlphabet(Alphabet<String> alphabet)
    {
        final int symbolNumber = alphabet.size() * alphabet.size();
        final Twin<String> epsilon = Tuples.twin(alphabet.epsilon(), alphabet.epsilon());
        final Alphabet.Builder<Twin<String>> builder = Alphabets.builder(symbolNumber, epsilon);
        final ImmutableSet<String> noEpsilonAlphabet = alphabet.noEpsilonSet();
        for (String s : noEpsilonAlphabet) {
            for (String t : noEpsilonAlphabet) {
                builder.add(Tuples.twin(s, t));
            }
        }

        return builder.build();
    }

    private static FSA<Twin<String>> composeTransducer(FSA<Twin<String>> one, FSA<Twin<String>> two,
                                                       Alphabet<Twin<String>> combinationAlphabet)
    {
        return FSAs.manipulator().makeProduct(one, two, combinationAlphabet, (s1, s2) -> {
            return s1.getTwo().equals(s2.getOne()) ? Tuples.twin(s1.getOne(), s2.getTwo()) : null;
        }, (stateMapping, builder) -> {
            final SetIterable<State> startStates = AutomatonManipulator
                .selectStatesFromProduct(stateMapping, one::isStartState, two::isStartState, AND);
            final SetIterable<State> acceptStates = AutomatonManipulator
                .selectStatesFromProduct(stateMapping, one::isAcceptState, two::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });
    }

    public Prover(Problem problem)
    {
        final FSAManipulator manipulator = FSAs.manipulator();
        initialConfigs = manipulator.determinize(problem.getInitialConfigurations());
        nonfinalConfigs = manipulator.makeComplement(problem.getFinalConfigurations());

        relationAlphabet = makeCombinationAlphabet(initialConfigs.getAlphabet());
        final FSA<Twin<String>> scheduler = problem.getSchedulerBehavior();
        final FSA<Twin<String>> process = problem.getProcessBehavior();
        transBehavior = manipulator.determinize(composeTransducer(scheduler, process, relationAlphabet));

        final IntIntPair invSearchBound = problem.getInvariantConfigSearchSpace();
        final IntIntPair relSearchBound = problem.getOrderRelationSearchSpace();
        invariantSizeBegin = invSearchBound.getOne();
        invariantSizeBound = invSearchBound.getTwo();
        relationSizeBegin = relSearchBound.getOne();
        relationSizeBound = relSearchBound.getTwo();
        solver = new Sat4jSolverAdapter();
    }

    private ImmutableList<String> verifyL1(FSA<String> invCandBar)
    {
        final FSAManipulator manipulator = FSAs.manipulator();
        final FSA<String> containmentCheck = manipulator.makeIntersection(initialConfigs, invCandBar);
        if (manipulator.checkLanguageEmpty(containmentCheck)) {
            return null;
        } else {
            return containmentCheck.enumerateOneShortestWord();
        }
    }

    private void applyCE1(FSAEncoding<String> invGuessing, ImmutableList<String> counterexample)
    {
        invGuessing.ensureAcceptingWord(counterexample);
    }

    private MutableSet<MutableList<String>> computeReversedPostImage(FSA<Twin<String>> target, State state,
                                                                     ImmutableList<String> word, boolean opposite,
                                                                     int capacity)
    {
        if (word.size() == 0) {
            final MutableSet<MutableList<String>> blankSingleton = UnifiedSet.newSet(1);
            blankSingleton.add(FastList.newList(capacity));
            return target.isAcceptState(state) ? blankSingleton : Sets.mutable.empty();
        }

        final DeltaFunction<Twin<String>> delta = target.getDeltaFunction();
        final int resultSizeBound = target.getAlphabetSize() * target.getStateNumber();
        final MutableSet<MutableList<String>> result = UnifiedSet.newSet(resultSizeBound); // upper bound

        String symbol1, symbol2, input, output;
        SetIterable<MutableList<String>> recursion;
        for (Twin<String> ioTrans : delta.enabledSymbolsOn(state)) {
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

    private <S> ImmutableList<S> reversedAndImmutable(MutableList<S> word)
    {
        return word.reverseThis().toImmutable();
    }

    private ImmutableSet<ImmutableList<String>> computePostImage(FSA<Twin<String>> target, ImmutableList<String> word)
    {
        final State startState = target.getStartState();

        return computeReversedPostImage(target, startState, word, false, word.size())
            .collect(this::reversedAndImmutable).toImmutable();
    }

    private ImmutableSet<ImmutableList<String>> computePreImage(FSA<Twin<String>> target, ImmutableList<String> word)
    {
        final State startState = target.getStartState();

        return computeReversedPostImage(target, startState, word, true, word.size()).collect(this::reversedAndImmutable)
                                                                                    .toImmutable();
    }

    private Pair<SetIterable<ImmutableList<String>>, ImmutableList<String>> verifyL2(FSA<String> invCand,
                                                                                     FSA<String> invCandBar)
    {
        final FSAManipulator manipulator = FSAs.manipulator();

        final FSA<String> postImage = manipulator
            .makeProduct(invCand, transBehavior, invCand.getAlphabet(), (s1, s2) -> {
                return s1.equals(s2.getOne()) ? s2.getTwo() : null;
            }, (stateMapping, builder) -> {
                final SetIterable<State> startStates = AutomatonManipulator
                    .selectStatesFromProduct(stateMapping, invCand::isStartState, transBehavior::isStartState, AND);
                final SetIterable<State> acceptStates = AutomatonManipulator
                    .selectStatesFromProduct(stateMapping, invCand::isAcceptState, transBehavior::isAcceptState, AND);
                builder.addStartStates(startStates);
                builder.addAcceptStates(acceptStates);
            });

        final FSA<String> containmentCheck = manipulator.makeIntersection(postImage, invCandBar);
        if (manipulator.checkLanguageEmpty(containmentCheck)) {
            return null;
        } else {
            final ImmutableList<String> witness = manipulator.determinize(containmentCheck).enumerateOneShortestWord();
            final SetIterable<ImmutableList<String>> preImage = computePreImage(transBehavior, witness);
            return Tuples.pair(preImage, witness);
        }
    }

    private void applyCE2(FSAEncoding<String> invGuessing,
                          Pair<SetIterable<ImmutableList<String>>, ImmutableList<String>> counterexample)
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

    private Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> verifyL3(FSA<Twin<String>> relCand)
    {
        final FSAManipulator manipulator = FSAs.manipulator();

        final FSA<Twin<String>> twoStepRelation = manipulator
            .makeProduct(relCand, relCand, relCand.getAlphabet(), (s1, s2) -> {
                return s1.getTwo().equals(s2.getOne()) ? Tuples.twin(s1.getOne(), s2.getTwo()) : null;
            }, (stateMapping, builder) -> {
                final SetIterable<State> startStates = AutomatonManipulator
                    .selectStatesFromProduct(stateMapping, relCand::isStartState, relCand::isStartState, AND);
                final SetIterable<State> acceptStates = AutomatonManipulator
                    .selectStatesFromProduct(stateMapping, relCand::isAcceptState, relCand::isAcceptState, AND);
                builder.addStartStates(startStates);
                builder.addAcceptStates(acceptStates);
            });

        final FSA<Twin<String>> relCandBar = manipulator.makeComplement(relCand);
        final FSA<Twin<String>> containmentCheck = manipulator.makeIntersection(twoStepRelation, relCandBar);
        if (manipulator.checkLanguageEmpty(containmentCheck)) {
            return null;
        } else {
            final ImmutableList<Twin<String>> witness = manipulator.determinize(containmentCheck)
                                                                   .enumerateOneShortestWord();
            final ImmutableList<String> x = witness.collect(Twin::getOne);
            final ImmutableList<String> z = witness.collect(Twin::getTwo);
            final ImmutableSet<ImmutableList<String>> xPostImage = computePostImage(relCand, x);
            final ImmutableSet<ImmutableList<String>> zPreImage = computePreImage(relCand, z);
            return Tuples.pair(witness, Sets.intersect(xPostImage.castToSet(), zPreImage.castToSet()));
        }
    }

    private <S> ImmutableList<Twin<S>> makeCombinedWord(ImmutableList<S> one, ImmutableList<S> two)
    {
        final int length = one.size();
        final MutableList<Twin<S>> result = FastList.newList(length);
        for (int i = 0; i < length; i++) {
            result.add(Tuples.twin(one.get(i), two.get(i)));
        }

        return result.toImmutable();
    }

    private void applyCE3(FSAEncoding<Twin<String>> relGuessing,
                          Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> counterexample)
    {
        final ImmutableList<String> x = counterexample.getOne().collect(Twin::getOne);
        final ImmutableList<String> z = counterexample.getOne().collect(Twin::getTwo);
        final int takenXZ = solver.newFreeVariables(1).getFirst();
        relGuessing.whetherAcceptWord(takenXZ, makeCombinedWord(x, z));
        for (ImmutableList<String> y : counterexample.getTwo()) {
            final int takenXY = solver.newFreeVariables(1).getFirst();
            final int takenYZ = solver.newFreeVariables(1).getFirst();

            relGuessing.whetherAcceptWord(takenXY, makeCombinedWord(x, y));
            relGuessing.whetherAcceptWord(takenYZ, makeCombinedWord(y, z));
            solver.addClause(-takenXY, -takenYZ, takenXZ);
        }
    }

    private <S, R> FSA<R> projectFSA(FSA<S> target, R epsilonSymbol, Function<S, R> projector)
    {
        final FSA.Builder<R> builder = FSAs.builder(target.getAlphabetSize(), epsilonSymbol, target.getStateNumber());
        builder.addStartStates(target.getStartStates());
        builder.addAcceptStates(target.getAcceptStates());
        final DeltaFunction<S> delta = target.getDeltaFunction();
        for (State dept : target.getStates()) {
            for (S symbol : delta.enabledSymbolsOn(dept)) {
                for (State dest : delta.successorsOf(dept, symbol)) {
                    builder.addTransition(dept, dest, projector.apply(symbol));
                }
            }
        }

        return builder.build();
    }

    private Pair<ImmutableList<String>, SetIterable<ImmutableList<String>>> verifyL4(FSA<String> invCand,
                                                                                     FSA<Twin<String>> relCand)
    {
        final FSAManipulator manipulator = FSAs.manipulator();
        final FSA<Twin<String>> smallerStepAvailable = manipulator.makeIntersection(transBehavior, relCand);
        final FSA<String> deptsWithSmallerStep = projectFSA(smallerStepAvailable,
                                                            invCand.getAlphabet().epsilon(), Pair::getOne);
        final FSA<String> deptsWithSmallerStepBar = manipulator.makeComplement(deptsWithSmallerStep);
        final FSA<String> nonfinalInv = manipulator.makeIntersection(invCand, nonfinalConfigs);
        final FSA<String> containmentCheck = manipulator.makeIntersection(nonfinalInv, deptsWithSmallerStepBar);
        if (manipulator.checkLanguageEmpty(containmentCheck)) {
            return null;
        } else {
            final ImmutableList<String> witness = containmentCheck.enumerateOneShortestWord().toImmutable();
            final SetIterable<ImmutableList<String>> postImage = computePostImage(transBehavior, witness);
            return Tuples.pair(witness, postImage);
        }
    }

    private void applyCE4(FSAEncoding<String> invGuessing, FSAEncoding<Twin<String>> relGuessing,
                          Pair<ImmutableList<String>, SetIterable<ImmutableList<String>>> counterexample)
    {
        final ImmutableList<String> x = counterexample.getOne();
        final int takenX = solver.newFreeVariables(1).getFirst();
        invGuessing.whetherAcceptWord(takenX, x);
        final SetIterable<ImmutableList<String>> possibleY = counterexample.getTwo();
        final ImmutableIntList takenAtLeastOneXY = possibleY.toList().collectInt(y -> {
            final int takenXY = solver.newFreeVariables(1).getFirst();
            relGuessing.whetherAcceptWord(takenXY, makeCombinedWord(x, y));
            return takenXY;
        }).toImmutable();
        solver.addClauseIf(takenX, takenAtLeastOneXY);
    }

    public void prove() throws SatSolverTimeoutException
    {
        final Alphabet<String> alphabet = initialConfigs.getAlphabet();
        final AlphabetIntEncoder<String> invSymbolEncoding = AlphabetIntEncoders.create(alphabet);
        final AlphabetIntEncoder<Twin<String>> relSymbolEncoding = AlphabetIntEncoders.create(relationAlphabet);
        final ImmutableSet<Twin<String>> reflexiveRelSymbols = alphabet.set().collect(s -> Tuples.twin(s, s));

        FSAEncoding<String> invGuessing;
        FSAEncoding<Twin<String>> relGuessing;
        FSA<String> invCand, invCandBar;
        FSA<Twin<String>> relCand;
        ImmutableList<String> counterexample1;
        Pair<SetIterable<ImmutableList<String>>, ImmutableList<String>> counterexample2;
        Pair<ImmutableList<Twin<String>>, SetIterable<ImmutableList<String>>> counterexample3;
        Pair<ImmutableList<String>, SetIterable<ImmutableList<String>>> counterexample4;
        final int stabilizerBound = invariantSizeBound * invariantSizeBound + relationSizeBound * relationSizeBound;
        for (int stabilizer = 1; stabilizer <= stabilizerBound; stabilizer++) {
            for (int invSearching = invariantSizeBegin; invSearching <= invariantSizeBound; invSearching++) {
                for (int relSearching = relationSizeBegin; relSearching <= relationSizeBound; relSearching++) {
                    final int stabilizerFactor = invSearching * invSearching + relSearching * relSearching;
                    if (stabilizerFactor != stabilizer) {
                        continue;
                    }

                    LOGGER.info("Searching advice bits in state spaces {} & {} ..", invSearching, relSearching);
                    invGuessing = new ReferenceFSAEncoding<>(solver, invSearching, invSymbolEncoding);
                    invGuessing.ensureNoDeadEndStates();
                    invGuessing.ensureNoUnreachableStates();
                    relGuessing = new ReferenceFSAEncoding<>(solver, relSearching, relSymbolEncoding);
                    relGuessing.ensureNoDeadEndStates();
                    relGuessing.ensureNoUnreachableStates();
                    relGuessing.ensureNoWordsPurelyMadeOf(reflexiveRelSymbols);

                    while (solver.findItSatisfiable()) {
                        invCand = invGuessing.resolveToFSA();
                        invCandBar = FSAs.manipulator().makeComplement(invCand);
                        relCand = relGuessing.resolveToFSA();

                        if ((counterexample1 = verifyL1(invCandBar)) != null) {
                            applyCE1(invGuessing, counterexample1);
                        }
                        if ((counterexample2 = verifyL2(invCand, invCandBar)) != null) {
                            applyCE2(invGuessing, counterexample2);
                        }
                        if ((counterexample3 = verifyL3(relCand)) != null) {
                            applyCE3(relGuessing, counterexample3);
                        }
                        if ((counterexample4 = verifyL4(invCand, relCand)) != null) {
                            applyCE4(invGuessing, relGuessing, counterexample4);
                        }

                        if (counterexample1 == null && counterexample2 == null && counterexample3 == null && counterexample4 == null) {
                            System.out.println("Proof found under the given search bound.");
                            return;
                        }
                    }

                    solver.reset();
                }
            }
        }

        System.out.println("No proof found under the given search bound.");
    }
}
