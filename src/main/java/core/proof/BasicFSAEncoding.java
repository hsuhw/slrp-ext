package core.proof;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.MutableState;
import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.proof.FSAEncoding;
import common.sat.SatSolver;
import common.util.Assert;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

public class BasicFSAEncoding<S> implements FSAEncoding<S>
{
    private static final int START_STATE_INDEX = 0;
    private static final int EPSILON_SYMBOL_INDEX = AlphabetIntEncoder.INT_EPSILON;

    private final SatSolver solver;
    private final int stateNumber;
    private final AlphabetIntEncoder<S> intAlphabet;
    private ImmutableIntList[][] transitionIndicators;
    private ImmutableIntList acceptStateIndicators;
    private boolean noUnreachableStateEnsured;
    private boolean noDeadEndStateEnsured;

    private void prepareTransitionIndicators()
    {
        final var symbolNumber = intAlphabet.size();
        transitionIndicators = new ImmutableIntList[stateNumber][symbolNumber];
        for (var state = 0; state < stateNumber; state++) {
            for (var symbol = 0; symbol < symbolNumber; symbol++) {
                transitionIndicators[state][symbol] = solver.newFreeVariables(stateNumber);
            }
        }
        final var transIndBegin = transitionIndicators[0][0].getFirst();
        final var transIndEnd = transitionIndicators[stateNumber - 1][symbolNumber - 1].getLast();
        solver.addClause(IntInterval.from(transIndBegin).to(transIndEnd)); // at least one transition overall
    }

    private void prepareAcceptStateIndicators()
    {
        acceptStateIndicators = solver.newFreeVariables(stateNumber);
        solver.addClause(acceptStateIndicators); // at least one accept state
    }

    private void ensureDeterminism()
    {
        for (var dept = 0; dept < stateNumber; dept++) {
            final var destsReachedByEpsilon = transitionIndicators[dept][EPSILON_SYMBOL_INDEX];
            destsReachedByEpsilon.forEach(solver::setLiteralFalsy); // no epsilon transition
            for (var symbol = 1; symbol < intAlphabet.size(); symbol++) {
                final var possibleDest = transitionIndicators[dept][symbol];
                solver.addClauseAtMost(1, possibleDest); // at most one transition, can be zero
            }
        }
    }

    private void applySymmetryBreakingHeuristics()
    {
        final var symbolNumber = intAlphabet.size();
        final var structuralOrder = new int[stateNumber - 1][symbolNumber + 1]; // skip the start state
        for (var i = 0; i < structuralOrder.length; i++) {
            final var state = i + 1;
            final var beAcceptState = acceptStateIndicators.get(state);
            structuralOrder[i][0] = beAcceptState; // assign to the highest digit (affects the most)
            for (var symbol = 0; symbol < symbolNumber; symbol++) {
                final var hasLoopOnSymbol = transitionIndicators[state][symbol].get(state);
                structuralOrder[i][1 + symbol] = hasLoopOnSymbol;
            }
        }
        for (var i = structuralOrder.length - 1; i > 0; i--) {
            solver.markAsGreaterEqualInBinary(structuralOrder[i], structuralOrder[i - 1]);
        }
    }

    private void applyLinearShapeRestriction()
    {
        for (var dept = 0; dept < stateNumber; dept++) {
            for (var dest = 0; dest < stateNumber; dest++) {
                for (var symbol = 1; symbol < intAlphabet.size(); symbol++) {
                    if (dest == dept || dest == (dept + 1) % stateNumber || dest == (dept + 2) % stateNumber) {
                        continue;
                    }
                    solver.setLiteralFalsy(transitionIndicators[dept][symbol].get(dest));
                }
            }
        }
    }

    public BasicFSAEncoding(SatSolver solver, int stateNumber, AlphabetIntEncoder<S> intAlphabet,
        boolean restrictsShape)
    {
        Assert.argumentNotNull(solver, intAlphabet);

        this.solver = solver;
        this.stateNumber = stateNumber;
        this.intAlphabet = intAlphabet;

        prepareTransitionIndicators();
        prepareAcceptStateIndicators();
        ensureDeterminism();
        if (restrictsShape) {
            applyLinearShapeRestriction();
        } else {
            applySymmetryBreakingHeuristics();
        }
    }

    public BasicFSAEncoding(SatSolver solver, int stateNumber, AlphabetIntEncoder<S> intAlphabet)
    {
        this(solver, stateNumber, intAlphabet, false);
    }

    private ImmutableIntList[] prepareDistanceIndicators()
    {
        final var distanceIndicators = new ImmutableIntList[stateNumber];
        for (var state = 0; state < stateNumber; state++) {
            final var possibleDistance = solver.newFreeVariables(stateNumber);
            distanceIndicators[state] = possibleDistance; // from 0 to n - 1
            solver.addClauseExactly(1, possibleDistance); // prevent any self-loop cause a distance
        }

        return distanceIndicators;
    }

    private void encodePossibleDistByTransIf(int required, int currState, ImmutableIntList[] distanceIndicators,
        TransitionSelector transitionSelector)
    {
        final var symbolNumber = intAlphabet.size();
        final var possibleDistByTrans = new ImmutableIntList[stateNumber][symbolNumber];
        for (var prevState = 0; prevState < stateNumber; prevState++) {
            for (var symbol = 0; symbol < symbolNumber; symbol++) {
                possibleDistByTrans[prevState][symbol] = solver.newFreeVariables(stateNumber);
                final var transCanCauseNoDist = possibleDistByTrans[prevState][symbol].get(0);
                solver.setLiteralFalsy(transCanCauseNoDist);
                for (var distNum = 1; distNum < stateNumber; distNum++) {
                    final var transCauseDistNum = possibleDistByTrans[prevState][symbol].get(distNum);
                    final var transBeAvailable = transitionSelector.take(prevState, currState, symbol);
                    final var prevBeNumMinusOne = distanceIndicators[prevState].get(distNum - 1);
                    final var currBeNum = distanceIndicators[currState].get(distNum);

                    // transCauseDistNum <--> transBeAvailable && prevBeNumMinusOne && currBeNum
                    solver.addImplications(transCauseDistNum, transBeAvailable, prevBeNumMinusOne, currBeNum);
                    solver.addClause(-transBeAvailable, -prevBeNumMinusOne, -currBeNum, transCauseDistNum);
                }
            }
        }
        // if required, at least one valid distance caused by the transitions
        final var distByTransBegin = possibleDistByTrans[0][0].getFirst();
        final var distByTransEnd = possibleDistByTrans[stateNumber - 1][symbolNumber - 1].getLast();
        solver.addClauseIf(required, IntInterval.from(distByTransBegin).to(distByTransEnd));
    }

    @Override
    public void ensureNoUnreachableState()
    {
        if (noUnreachableStateEnsured) {
            return;
        }

        final var distFromStartIndicators = prepareDistanceIndicators();
        final var startStateDistBeZero = distFromStartIndicators[START_STATE_INDEX].get(0);
        solver.setLiteralTruthy(startStateDistBeZero);
        final var notStartState = solver.newFreeVariable();
        solver.setLiteralTruthy(notStartState);
        for (var state = 1; state < stateNumber; state++) { // skip the start state
            final var distCanBeZero = distFromStartIndicators[state].get(0);
            solver.setLiteralFalsy(distCanBeZero);
            encodePossibleDistByTransIf(notStartState, state, distFromStartIndicators,
                                        (dept, dest, symbol) -> transitionIndicators[dept][symbol].get(dest));
        }

        noUnreachableStateEnsured = true;
    }

    @Override
    public void ensureNoDeadEndState()
    {
        if (noDeadEndStateEnsured) {
            return;
        }

        var distFromAcceptIndicators = prepareDistanceIndicators();
        for (var state = 0; state < stateNumber; state++) {
            final var takenAsAcceptState = acceptStateIndicators.get(state);
            final var distBeZero = distFromAcceptIndicators[state].get(0);
            solver.markAsEquivalent(takenAsAcceptState, distBeZero);
            encodePossibleDistByTransIf(-takenAsAcceptState, state, distFromAcceptIndicators,
                                        (dept, dest, symbol) -> transitionIndicators[dest][symbol].get(dept));
        }

        noDeadEndStateEnsured = true;
    }

    private CertainWord makeWord(ListIterable<S> definition)
    {
        final var epsilon = intAlphabet.originEpsilon();
        final var givenWord = definition.select(symbol -> symbol != epsilon);
        final var word = new CertainWord(givenWord.size());
        givenWord.forEachWithIndex((symbol, pos) -> word.setCharacterAt(pos, symbol));

        return word;
    }

    private void ensureAcceptingIf(int activated, CertainWord word)
    {
        // define each possible step over states on each input symbol read
        final var stepIndicators = new ImmutableIntList[word.length + 1];
        for (var readHead = 0; readHead < word.length + 1; readHead++) {
            final var possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            solver.addClause(possibleStateStepping);
        }
        final var initialStepBeStartState = stepIndicators[0].get(START_STATE_INDEX);
        solver.setLiteralTruthy(initialStepBeStartState);

        // make the taken steps represent the given word
        for (var readHead = 0; readHead < word.length; readHead++) {
            final var possibleSymbol = word.characterIndicators[readHead];
            for (var qi = 0; qi < stateNumber; qi++) {
                final var takenQiAsCurr = stepIndicators[readHead].get(qi);
                for (var qj = 0; qj < stateNumber; qj++) {
                    final var takenQjAsNext = stepIndicators[readHead + 1].get(qj);
                    for (var s = 0; s < intAlphabet.size(); s++) {
                        final var sBeSymbol = possibleSymbol.get(s);
                        final var transBeAvailable = transitionIndicators[qi][s].get(qj);
                        solver.addClauseIf(activated, -takenQiAsCurr, -takenQjAsNext, -sBeSymbol, transBeAvailable);
                    }
                }
            }
        }

        // make the taken steps form an accepting path
        final var possibleLastStep = stepIndicators[word.length];
        for (var state = 0; state < stateNumber; state++) {
            final var ifTakenAsLastStep = possibleLastStep.get(state);
            final var beAcceptState = acceptStateIndicators.get(state);
            solver.addImplicationIf(activated, ifTakenAsLastStep, beAcceptState);
        }
    }

    @Override
    public void ensureAccepting(ListIterable<S> word)
    {
        final var activated = solver.newFreeVariable();
        ensureAcceptingIf(activated, makeWord(word));
        solver.setLiteralTruthy(activated);
    }

    private void prepareFailureIndicators(ImmutableIntList failAtIndicators, ImmutableIntList failedAlreadyIndicators)
    {
        failAtIndicators.forEachWithIndex((failAtCurr, i) -> {
            final var failedAlreadyAtCurr = failedAlreadyIndicators.get(i);
            final var failedAlreadyAtNext = failedAlreadyIndicators.get(i + 1);

            // failedAlreadyAtNext <--> failAtCurr || failedAlreadyAtCurr
            solver.addImplication(failAtCurr, failedAlreadyAtNext);
            solver.addImplication(failedAlreadyAtCurr, failedAlreadyAtNext);
            solver.addClause(-failedAlreadyAtNext, failedAlreadyAtCurr, failAtCurr);
        });
        final var initialStepNeverFailedAlready = -failedAlreadyIndicators.get(0);
        solver.setLiteralTruthy(initialStepNeverFailedAlready);
    }

    private void ensureNotAcceptWordIf(int activated, CertainWord word)
    {
        // prepare the fail bits for each step of the input read
        final var failAtIndicators = solver.newFreeVariables(word.length);
        final var failedAlreadyIndicators = solver.newFreeVariables(word.length + 1);
        prepareFailureIndicators(failAtIndicators, failedAlreadyIndicators);

        // define each possible step over states on each input symbol read
        final var stepIndicators = new ImmutableIntList[word.length + 1];
        for (var readHead = 0; readHead < word.length + 1; readHead++) {
            final var possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            final var stillNotFailed = -failedAlreadyIndicators.get(readHead);
            solver.addClauseIf(stillNotFailed, possibleStateStepping);
        }
        final var initialStepBeStartState = stepIndicators[0].get(START_STATE_INDEX);
        solver.setLiteralTruthy(initialStepBeStartState);

        // make the taken steps form a non-accepting path
        for (var readHead = 0; readHead < word.length; readHead++) {
            final var alreadyFailed = failedAlreadyIndicators.get(readHead);
            final var failsHere = failAtIndicators.get(readHead);
            final var possibleSymbol = word.characterIndicators[readHead];
            for (var qi = 0; qi < stateNumber; qi++) {
                final var takenQiAsCurr = stepIndicators[readHead].get(qi);
                for (var qj = 0; qj < stateNumber; qj++) {
                    final var takenQjAsNext = stepIndicators[readHead + 1].get(qj);
                    for (var s = 0; s < intAlphabet.size(); s++) {
                        final var sBeSymbol = possibleSymbol.get(s);
                        final var transBeAvailable = transitionIndicators[qi][s].get(qj);
                        solver.addClauseIf(activated, alreadyFailed, failsHere, -takenQiAsCurr, -takenQjAsNext,
                                           -sBeSymbol, transBeAvailable);
                        solver.addClauseIf(activated, alreadyFailed, -failsHere, -takenQiAsCurr, -sBeSymbol,
                                           -transBeAvailable);
                    }
                }
            }
        }
        final var failedBeforeFinal = failedAlreadyIndicators.get(word.length);
        final var possibleLastStep = stepIndicators[word.length];
        for (var state = 0; state < stateNumber; state++) {
            final var takenAsLastStep = possibleLastStep.get(state);
            final var notAcceptState = -acceptStateIndicators.get(state);
            solver.addClauseIf(activated, failedBeforeFinal, -takenAsLastStep, notAcceptState);
        }
    }

    @Override
    public void ensureNoAccepting(ListIterable<S> word)
    {
        final var activated = solver.newFreeVariable();
        ensureNotAcceptWordIf(activated, makeWord(word));
        solver.setLiteralTruthy(activated);
    }

    @Override
    public void ensureAcceptingIfOnlyIf(int indicator, ListIterable<S> word)
    {
        final var encodedWord = makeWord(word);
        ensureAcceptingIf(indicator, encodedWord);
        ensureNotAcceptWordIf(-indicator, encodedWord);
    }

    @Override
    public CertainWord ensureAcceptingCertainWordIf(int indicator, int length)
    {
        final var certainWord = new CertainWord(length);
        ensureAcceptingIf(indicator, certainWord);

        return certainWord;
    }

    @Override
    public void ensureNoWordPurelyMadeOf(SetIterable<S> symbols)
    {
        final var encodedSymbols = intAlphabet.encode(symbols.toList().toImmutable());

        if (encodedSymbols.contains(EPSILON_SYMBOL_INDEX)) {
            solver.setLiteralsFalsy(acceptStateIndicators.get(0));
        }

        final var canBePurelyMadeUntil = solver.newFreeVariables(stateNumber + 1);
        final var initialStepAlwaysPossible = canBePurelyMadeUntil.get(START_STATE_INDEX);
        solver.setLiteralTruthy(initialStepAlwaysPossible);

        final var startStatePurityBroken = -canBePurelyMadeUntil.get(stateNumber);
        for (var qi = 0; qi < stateNumber; qi++) {
            final var purityAlreadyBroken = -canBePurelyMadeUntil.get(qi);
            final var ifIsAcceptState = acceptStateIndicators.get(qi);
            if (qi == 0) {
                solver.addImplication(ifIsAcceptState, startStatePurityBroken);
            } else {
                solver.addImplication(ifIsAcceptState, purityAlreadyBroken);
            }
            for (var qj = 0; qj < stateNumber; qj++) {
                final var stillPossibleAtQj = qj == 0 ? -startStatePurityBroken : canBePurelyMadeUntil.get(qj);
                for (var s = 0; s < encodedSymbols.size(); s++) {
                    final var transAvailable = transitionIndicators[qi][encodedSymbols.get(s)].get(qj);
                    solver.addImplicationIf(-purityAlreadyBroken, transAvailable, stillPossibleAtQj);
                }
            }
        }
    }

    @Override
    public void blockCurrentInstance()
    {
        // collect which to be blocked
        final var truthyIndicators = solver.getModelTruthyVariables();
        final MutableIntSet blockingTrack = new IntHashSet(truthyIndicators.size());
        acceptStateIndicators.forEach(acceptIndicator -> blockingTrack
            .add(truthyIndicators.contains(acceptIndicator) ? acceptIndicator : -acceptIndicator));
        for (var qi = 0; qi < stateNumber; qi++) {
            for (var qj = 0; qj < stateNumber; qj++) {
                for (var s = 0; s < intAlphabet.size(); s++) {
                    final var transIndicator = transitionIndicators[qi][s].get(qj);
                    blockingTrack.add(truthyIndicators.contains(transIndicator) ? transIndicator : -transIndicator);
                }
            }
        }

        solver.addClauseBlocking(blockingTrack.toArray());
    }

    @Override
    public FSA<S> resolve()
    {
        if (!solver.findItSatisfiable()) {
            return null;
        }

        // decode states
        final var truthyIndicators = solver.getModelTruthyVariables();
        final var result = FSAs.create(intAlphabet.originAlphabet(), stateNumber);
        final MutableList<MutableState<S>> states = FastList.newList(stateNumber);
        states.add(START_STATE_INDEX, result.startState());
        for (var i = 1; i < stateNumber; i++) {
            states.add(i, result.newState("s" + i));
        }
        acceptStateIndicators.forEachWithIndex((indicator, stateIndex) -> {
            if (truthyIndicators.contains(indicator)) {
                result.setAsAccept(states.get(stateIndex));
            }
        });

        // decode transitions
        for (var qi = 0; qi < stateNumber; qi++) {
            for (var qj = 0; qj < stateNumber; qj++) {
                for (var s = 0; s < intAlphabet.size(); s++) {
                    if (truthyIndicators.contains(transitionIndicators[qi][s].get(qj))) {
                        result.addTransition(states.get(qi), states.get(qj), intAlphabet.decode(s));
                    }
                }
            }
        }

        return result;
    }


    @FunctionalInterface
    private interface TransitionSelector
    {
        int take(int dept, int dest, int symbol);
    }

    private class CertainWord implements FSAEncoding.CertainWord<S>
    {
        private final int length;
        private final ImmutableIntList[] characterIndicators;

        private CertainWord(int length)
        {
            this.length = length;
            characterIndicators = new ImmutableIntList[length];

            final var symbolNumber = intAlphabet.size();
            for (var i = 0; i < length; i++) {
                final var possibleSymbol = solver.newFreeVariables(symbolNumber);
                final var characterCanBeEpsilon = possibleSymbol.get(EPSILON_SYMBOL_INDEX);
                solver.setLiteralFalsy(characterCanBeEpsilon);
                solver.addClause(possibleSymbol);
                characterIndicators[i] = possibleSymbol;
            }
        }

        @Override
        public int length()
        {
            return length;
        }

        @Override
        public Alphabet<S> alphabet()
        {
            return intAlphabet.originAlphabet();
        }

        @Override
        public int getCharacterIndicator(int pos, S symbol)
        {
            if (pos >= length) {
                throw new IndexOutOfBoundsException("non-existing character position");
            }
            if (symbol.equals(intAlphabet.originEpsilon())) {
                throw new IllegalArgumentException("epsilon symbol not allowed");
            }

            return characterIndicators[pos].get(intAlphabet.encode(symbol));
        }

        @Override
        public void setCharacterAt(int pos, S symbol)
        {
            if (pos >= length) {
                throw new IndexOutOfBoundsException("non-existing character position");
            }
            if (symbol.equals(intAlphabet.originEpsilon())) {
                throw new IllegalArgumentException("epsilon symbol not allowed");
            }

            solver.setLiteralTruthy(characterIndicators[pos].get(intAlphabet.encode(symbol)));
        }

        @Override
        public void ensureAcceptedBy(FSA<S> fsa)
        {
            final var dfa = fsa.determinize();
            final var stateNumber = dfa.states().size();
            final var stateDecoder = dfa.states().toList();
            final MutableObjectIntMap<State<S>> stateEncoder = new ObjectIntHashMap<>(stateNumber);
            stateDecoder.forEachWithIndex(stateEncoder::put);

            // define each possible step over the DFA's states with each character of the word
            final var stepIndicators = new ImmutableIntList[length + 1];
            for (var pos = 0; pos < length + 1; pos++) {
                final var possibleStateStepping = solver.newFreeVariables(stateNumber);
                stepIndicators[pos] = possibleStateStepping;
                solver.addClause(possibleStateStepping);
            }
            final var initialStepBeStartState = stepIndicators[0].get(stateEncoder.get(dfa.startState()));
            solver.setLiteralTruthy(initialStepBeStartState);

            // make the taken steps represent the word
            final var symbols = alphabet().asSet();
            for (var pos = 0; pos < length; pos++) {
                final var possibleSymbol = characterIndicators[pos];
                for (var qi = 0; qi < stateNumber; qi++) {
                    final var takenQiAsCurr = stepIndicators[pos].get(qi);
                    final var stateQi = stateDecoder.get(qi);
                    for (var qj = 0; qj < stateNumber; qj++) {
                        final var takenQjAsNext = stepIndicators[pos + 1].get(qj);
                        final var stateQj = stateDecoder.get(qj);
                        final var enabledQiToQjArc = stateQi.enabledSymbolsTo(stateQj);
                        for (var symbol : symbols) {
                            if (enabledQiToQjArc.contains(symbol)) { // consider the disabled symbols
                                continue;
                            }
                            final var posBeDisableSymbol = possibleSymbol.get(intAlphabet.encode(symbol));
                            solver.addClause(-takenQiAsCurr, -takenQjAsNext, -posBeDisableSymbol);
                        }
                    }
                }
            }

            // make the taken steps form an accepting path
            final var possibleLastStep = stepIndicators[length];
            for (var nonAcceptState : dfa.nonAcceptStates()) {
                final var finalStepBeNonAccept = possibleLastStep.get(stateEncoder.get(nonAcceptState));
                solver.setLiteralFalsy(finalStepBeNonAccept);
            }
        }
    }
}
