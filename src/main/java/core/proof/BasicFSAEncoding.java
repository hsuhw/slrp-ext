package core.proof;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.proof.FSAEncoding;
import api.proof.SatSolver;
import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

import static api.util.Values.Direction;
import static api.util.Values.Direction.BACKWARD;
import static api.util.Values.Direction.FORWARD;

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
        final int symbolNumber = intAlphabet.size();
        transitionIndicators = new ImmutableIntList[stateNumber][symbolNumber];
        for (int state = 0; state < stateNumber; state++) {
            for (int symbol = 0; symbol < symbolNumber; symbol++) {
                transitionIndicators[state][symbol] = solver.newFreeVariables(stateNumber);
            }
        }
        final int transIndBegin = transitionIndicators[0][0].getFirst();
        final int transIndEnd = transitionIndicators[stateNumber - 1][symbolNumber - 1].getLast();
        solver.addClause(IntInterval.from(transIndBegin).to(transIndEnd)); // at least one transition overall
    }

    private void prepareAcceptStateIndicators()
    {
        acceptStateIndicators = solver.newFreeVariables(stateNumber);
        solver.addClause(acceptStateIndicators); // at least one accept state
    }

    private void ensureDeterminism()
    {
        for (int dept = 0; dept < stateNumber; dept++) {
            final ImmutableIntList destsReachedByEpsilon = transitionIndicators[dept][EPSILON_SYMBOL_INDEX];
            destsReachedByEpsilon.forEach(solver::setLiteralFalsy); // no epsilon transition
            for (int symbol = 1; symbol < intAlphabet.size(); symbol++) {
                final ImmutableIntList possibleDest = transitionIndicators[dept][symbol];
                solver.addClauseAtMost(1, possibleDest); // at most one transition, can be zero
            }
        }
    }

    private void applySymmetryBreakingHeuristics()
    {
        final int symbolNumber = intAlphabet.size();
        final int[][] structuralOrder = new int[stateNumber - 1][symbolNumber + 1]; // skip the start state
        for (int i = 0; i < structuralOrder.length; i++) {
            final int state = i + 1;
            final int beAcceptState = acceptStateIndicators.get(state);
            structuralOrder[i][0] = beAcceptState; // assign to the highest digit (affects the most)
            for (int symbol = 0; symbol < symbolNumber; symbol++) {
                final int hasLoopOnSymbol = transitionIndicators[state][symbol].get(state);
                structuralOrder[i][1 + symbol] = hasLoopOnSymbol;
            }
        }
        for (int i = structuralOrder.length - 1; i > 0; i--) {
            solver.markAsGreaterEqualInBinary(structuralOrder[i], structuralOrder[i - 1]);
        }
    }

    public BasicFSAEncoding(SatSolver solver, int stateNumber, AlphabetIntEncoder<S> intAlphabet)
    {
        Assertions.argumentNotNull(solver, intAlphabet);

        this.solver = solver;
        this.stateNumber = stateNumber;
        this.intAlphabet = intAlphabet;

        prepareTransitionIndicators();
        prepareAcceptStateIndicators();
        ensureDeterminism();
        applySymmetryBreakingHeuristics();
    }

    private ImmutableIntList[] prepareDistanceIndicators()
    {
        final ImmutableIntList[] distanceIndicators = new ImmutableIntList[stateNumber];
        for (int state = 0; state < stateNumber; state++) {
            final ImmutableIntList possibleDistance = solver.newFreeVariables(stateNumber);
            distanceIndicators[state] = possibleDistance; // from 0 to n - 1
            solver.addClauseExactly(1, possibleDistance); // prevent any self-loop cause a distance
        }

        return distanceIndicators;
    }

    private void encodePossibleDistByTransIf(int required, int currState, ImmutableIntList[] distanceIndicators,
                                             Direction direction)
    {
        final int symbolNumber = intAlphabet.size();
        final ImmutableIntList[][] possibleDistByTrans = new ImmutableIntList[stateNumber][symbolNumber];
        for (int prevState = 0; prevState < stateNumber; prevState++) {
            for (int symbol = 0; symbol < symbolNumber; symbol++) {
                possibleDistByTrans[prevState][symbol] = solver.newFreeVariables(stateNumber);
                final int transCanCauseNoDist = possibleDistByTrans[prevState][symbol].get(0);
                solver.setLiteralFalsy(transCanCauseNoDist);
                for (int distNum = 1; distNum < stateNumber; distNum++) {
                    final int transCauseDistNum = possibleDistByTrans[prevState][symbol].get(distNum);
                    final int transBeAvailable = direction == Direction.FORWARD
                                                 ? transitionIndicators[prevState][symbol].get(currState)
                                                 : transitionIndicators[currState][symbol].get(prevState);
                    final int prevBeNumMinusOne = distanceIndicators[prevState].get(distNum - 1);
                    final int currBeNum = distanceIndicators[currState].get(distNum);

                    // transCauseDistNum <--> transBeAvailable && prevBeNumMinusOne && currBeNum
                    solver.addImplications(transCauseDistNum, transBeAvailable, prevBeNumMinusOne, currBeNum);
                    solver.addClause(-transBeAvailable, -prevBeNumMinusOne, -currBeNum, transCauseDistNum);
                }
            }
        }
        // if required, at least one valid distance caused by the transitions
        final int distByTransBegin = possibleDistByTrans[0][0].getFirst();
        final int distByTransEnd = possibleDistByTrans[stateNumber - 1][symbolNumber - 1].getLast();
        solver.addClauseIf(required, IntInterval.from(distByTransBegin).to(distByTransEnd));
    }

    @Override
    public void ensureNoUnreachableState()
    {
        if (noUnreachableStateEnsured) {
            return;
        }

        final ImmutableIntList[] distFromStartIndicators = prepareDistanceIndicators();
        final int startStateDistBeZero = distFromStartIndicators[START_STATE_INDEX].get(0);
        solver.setLiteralTruthy(startStateDistBeZero);
        final int notStartState = solver.newFreeVariable();
        solver.setLiteralTruthy(notStartState);
        for (int state = 1; state < stateNumber; state++) { // skip the start state
            final int distCanBeZero = distFromStartIndicators[state].get(0);
            solver.setLiteralFalsy(distCanBeZero);
            encodePossibleDistByTransIf(notStartState, state, distFromStartIndicators, FORWARD);
        }

        noUnreachableStateEnsured = true;
    }

    @Override
    public void ensureNoDeadEndState()
    {
        if (noDeadEndStateEnsured) {
            return;
        }

        ImmutableIntList[] distFromAcceptIndicators = prepareDistanceIndicators();
        for (int state = 0; state < stateNumber; state++) {
            final int takenAsAcceptState = acceptStateIndicators.get(state);
            final int distBeZero = distFromAcceptIndicators[state].get(0);
            solver.markAsEquivalent(takenAsAcceptState, distBeZero);
            encodePossibleDistByTransIf(-takenAsAcceptState, state, distFromAcceptIndicators, BACKWARD);
        }

        noDeadEndStateEnsured = true;
    }

    private CertainWord makeWord(ImmutableList<S> definition)
    {
        final S epsilon = intAlphabet.originEpsilon();
        final ImmutableList<S> givenWord = definition.select(symbol -> symbol != epsilon);
        final CertainWord word = new CertainWord(givenWord.size());
        givenWord.forEachWithIndex((symbol, pos) -> word.setCharacterAt(pos, symbol));

        return word;
    }

    private void ensureAcceptingIf(int activated, CertainWord word)
    {
        // define each possible step over states on each input symbol read
        final ImmutableIntList[] stepIndicators = new ImmutableIntList[word.length + 1];
        for (int readHead = 0; readHead < word.length + 1; readHead++) {
            final ImmutableIntList possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            solver.addClause(possibleStateStepping);
        }
        final int initialStepBeStartState = stepIndicators[0].get(START_STATE_INDEX);
        solver.setLiteralTruthy(initialStepBeStartState);

        // make the taken steps represent the given word
        for (int readHead = 0; readHead < word.length; readHead++) {
            final ImmutableIntList possibleSymbol = word.characterIndicators[readHead];
            for (int qi = 0; qi < stateNumber; qi++) {
                final int takenQiAsCurr = stepIndicators[readHead].get(qi);
                for (int qj = 0; qj < stateNumber; qj++) {
                    final int takenQjAsNext = stepIndicators[readHead + 1].get(qj);
                    for (int s = 0; s < intAlphabet.size(); s++) {
                        final int sBeSymbol = possibleSymbol.get(s);
                        final int transBeAvailable = transitionIndicators[qi][s].get(qj);
                        solver.addClauseIf(activated, -takenQiAsCurr, -takenQjAsNext, -sBeSymbol, transBeAvailable);
                    }
                }
            }
        }

        // make the taken steps form an accepting path
        final ImmutableIntList possibleLastStep = stepIndicators[word.length];
        for (int state = 0; state < stateNumber; state++) {
            final int ifTakenAsLastStep = possibleLastStep.get(state);
            final int beAcceptState = acceptStateIndicators.get(state);
            solver.addImplicationIf(activated, ifTakenAsLastStep, beAcceptState);
        }
    }

    @Override
    public void ensureAccepting(ImmutableList<S> word)
    {
        final int activated = solver.newFreeVariable();
        ensureAcceptingIf(activated, makeWord(word));
        solver.setLiteralTruthy(activated);
    }

    private void prepareFailureIndicators(ImmutableIntList failAtIndicators, ImmutableIntList failedAlreadyIndicators)
    {
        failAtIndicators.forEachWithIndex((failAtCurr, i) -> {
            final int failedAlreadyAtCurr = failedAlreadyIndicators.get(i);
            final int failedAlreadyAtNext = failedAlreadyIndicators.get(i + 1);

            // failedAlreadyAtNext <--> failAtCurr || failedAlreadyAtCurr
            solver.addImplication(failAtCurr, failedAlreadyAtNext);
            solver.addImplication(failedAlreadyAtCurr, failedAlreadyAtNext);
            solver.addClause(-failedAlreadyAtNext, failedAlreadyAtCurr, failAtCurr);
        });
        final int initialStepNeverFailedAlready = -failedAlreadyIndicators.get(0);
        solver.setLiteralTruthy(initialStepNeverFailedAlready);
    }

    private void ensureNotAcceptWordIf(int activated, CertainWord word)
    {
        // prepare the fail bits for each step of the input read
        final ImmutableIntList failAtIndicators = solver.newFreeVariables(word.length);
        final ImmutableIntList failedAlreadyIndicators = solver.newFreeVariables(word.length + 1);
        prepareFailureIndicators(failAtIndicators, failedAlreadyIndicators);

        // define each possible step over states on each input symbol read
        final ImmutableIntList[] stepIndicators = new ImmutableIntList[word.length + 1];
        for (int readHead = 0; readHead < word.length + 1; readHead++) {
            final ImmutableIntList possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            final int stillNotFailed = -failedAlreadyIndicators.get(readHead);
            solver.addClauseIf(stillNotFailed, possibleStateStepping);
        }
        final int initialStepBeStartState = stepIndicators[0].get(START_STATE_INDEX);
        solver.setLiteralTruthy(initialStepBeStartState);

        // make the taken steps form a non-accepting path
        for (int readHead = 0; readHead < word.length; readHead++) {
            final int alreadyFailed = failedAlreadyIndicators.get(readHead);
            final int failsHere = failAtIndicators.get(readHead);
            final ImmutableIntList possibleSymbol = word.characterIndicators[readHead];
            for (int qi = 0; qi < stateNumber; qi++) {
                final int takenQiAsCurr = stepIndicators[readHead].get(qi);
                for (int qj = 0; qj < stateNumber; qj++) {
                    final int takenQjAsNext = stepIndicators[readHead + 1].get(qj);
                    for (int s = 0; s < intAlphabet.size(); s++) {
                        final int sBeSymbol = possibleSymbol.get(s);
                        final int transBeAvailable = transitionIndicators[qi][s].get(qj);
                        solver.addClauseIf(activated, alreadyFailed, failsHere, -takenQiAsCurr, -takenQjAsNext,
                                           -sBeSymbol, transBeAvailable);
                        solver.addClauseIf(activated, alreadyFailed, -failsHere, -takenQiAsCurr, -sBeSymbol,
                                           -transBeAvailable);
                    }
                }
            }
        }
        final int failedBeforeFinal = failedAlreadyIndicators.get(word.length);
        final ImmutableIntList possibleLastStep = stepIndicators[word.length];
        for (int state = 0; state < stateNumber; state++) {
            final int takenAsLastStep = possibleLastStep.get(state);
            final int notAcceptState = -acceptStateIndicators.get(state);
            solver.addClauseIf(activated, failedBeforeFinal, -takenAsLastStep, notAcceptState);
        }
    }

    @Override
    public void ensureNoAccepting(ImmutableList<S> word)
    {
        final int activated = solver.newFreeVariable();
        ensureNotAcceptWordIf(activated, makeWord(word));
        solver.setLiteralTruthy(activated);
    }

    @Override
    public void ensureAcceptingIfOnlyIf(int indicator, ImmutableList<S> word)
    {
        final CertainWord encodedWord = makeWord(word);
        ensureAcceptingIf(indicator, encodedWord);
        ensureNotAcceptWordIf(-indicator, encodedWord);
    }

    @Override
    public CertainWord ensureAcceptingCertainWordIf(int indicator, int length)
    {
        final CertainWord certainWord = new CertainWord(length);
        ensureAcceptingIf(indicator, certainWord);

        return certainWord;
    }

    @Override
    public void ensureNoWordPurelyMadeOf(ImmutableSet<S> symbols)
    {
        final ImmutableIntList encodedSymbols = intAlphabet.encode(symbols.toList().toImmutable());

        if (encodedSymbols.contains(EPSILON_SYMBOL_INDEX)) {
            solver.setLiteralsFalsy(acceptStateIndicators.get(0));
        }

        final ImmutableIntList canBePurelyMadeUntil = solver.newFreeVariables(stateNumber + 1);
        final int initialStepAlwaysPossible = canBePurelyMadeUntil.get(START_STATE_INDEX);
        solver.setLiteralTruthy(initialStepAlwaysPossible);

        final int startStatePurityBroken = -canBePurelyMadeUntil.get(stateNumber);
        for (int qi = 0; qi < stateNumber; qi++) {
            final int purityAlreadyBroken = -canBePurelyMadeUntil.get(qi);
            final int ifIsAcceptState = acceptStateIndicators.get(qi);
            if (qi == 0) {
                solver.addImplication(ifIsAcceptState, startStatePurityBroken);
            } else {
                solver.addImplication(ifIsAcceptState, purityAlreadyBroken);
            }
            for (int qj = 0; qj < stateNumber; qj++) {
                final int stillPossibleAtQj = qj == 0 ? -startStatePurityBroken : canBePurelyMadeUntil.get(qj);
                for (int s = 0; s < encodedSymbols.size(); s++) {
                    final int transAvailable = transitionIndicators[qi][encodedSymbols.get(s)].get(qj);
                    solver.addImplicationIf(-purityAlreadyBroken, transAvailable, stillPossibleAtQj);
                }
            }
        }
    }

    @Override
    public void blockCurrentInstance()
    {
        // collect which to be blocked
        final ImmutableIntSet truthyIndicators = solver.getModelTruthyVariables();
        final MutableIntSet blockingTrack = new IntHashSet(truthyIndicators.size());
        acceptStateIndicators.forEach(acceptIndicator -> {
            blockingTrack.add(truthyIndicators.contains(acceptIndicator) ? acceptIndicator : -acceptIndicator);
        });
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int s = 0; s < intAlphabet.size(); s++) {
                    final int transIndicator = transitionIndicators[qi][s].get(qj);
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
        final ImmutableIntSet truthyIndicators = solver.getModelTruthyVariables();
        final FSA.Builder<S> builder = FSAs.builder(stateNumber, intAlphabet.size(), intAlphabet.originEpsilon());
        final State[] states = new State[stateNumber];
        for (int i = 0; i < stateNumber; i++) {
            states[i] = States.create("s" + i);
        }
        builder.addStartState(states[START_STATE_INDEX]);
        acceptStateIndicators.forEachWithIndex((indicator, stateIndex) -> {
            if (truthyIndicators.contains(indicator)) {
                builder.addAcceptState(states[stateIndex]);
            }
        });

        // decode transitions
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int s = 0; s < intAlphabet.size(); s++) {
                    if (truthyIndicators.contains(transitionIndicators[qi][s].get(qj))) {
                        builder.addTransition(states[qi], states[qj], intAlphabet.decode(s));
                    }
                }
            }
        }

        return builder.buildWith(intAlphabet.originAlphabet());
    }

    private class CertainWord implements FSAEncoding.CertainWord<S>
    {
        private final int length;
        private final ImmutableIntList[] characterIndicators;

        private CertainWord(int length)
        {
            this.length = length;
            characterIndicators = new ImmutableIntList[length];

            final int symbolNumber = intAlphabet.size();
            for (int i = 0; i < length; i++) {
                final ImmutableIntList possibleSymbol = solver.newFreeVariables(symbolNumber);
                final int characterCanBeEpsilon = possibleSymbol.get(EPSILON_SYMBOL_INDEX);
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
            final FSA<S> dfa = FSAs.determinize(fsa);
            final int stateNumber = dfa.states().size();
            final MutableList<State> stateDecoder = dfa.states().toList();
            final MutableObjectIntMap<State> stateEncoder = new ObjectIntHashMap<>(stateNumber);
            stateDecoder.forEachWithIndex(stateEncoder::put);
            final TransitionGraph<State, S> delta = fsa.transitionGraph();

            // define each possible step over the DFA's states with each character of the word
            final ImmutableIntList[] stepIndicators = new ImmutableIntList[length + 1];
            for (int pos = 0; pos < length + 1; pos++) {
                final ImmutableIntList possibleStateStepping = solver.newFreeVariables(stateNumber);
                stepIndicators[pos] = possibleStateStepping;
                solver.addClause(possibleStateStepping);
            }
            final int initialStepBeStartState = stepIndicators[0].get(stateEncoder.get(dfa.startState()));
            solver.setLiteralTruthy(initialStepBeStartState);

            // make the taken steps represent the word
            for (int pos = 0; pos < length; pos++) {
                final ImmutableIntList possibleSymbol = characterIndicators[pos];
                for (int qi = 0; qi < stateNumber; qi++) {
                    final int takenQiAsCurr = stepIndicators[pos].get(qi);
                    final State stateQi = stateDecoder.get(qi);
                    for (int qj = 0; qj < stateNumber; qj++) {
                        final int takenQjAsNext = stepIndicators[pos + 1].get(qj);
                        final State stateQj = stateDecoder.get(qj);
                        final SetIterable<S> enabledQiToQjArc = delta.arcLabelsOn(stateQi, stateQj);
                        for (S disableSymbol : alphabet().asSet().newWithoutAll(enabledQiToQjArc)) {
                            final int posBeDisableSymbol = possibleSymbol.get(intAlphabet.encode(disableSymbol));
                            solver.addClause(-takenQiAsCurr, -takenQjAsNext, -posBeDisableSymbol);
                        }
                    }
                }
            }

            // make the taken steps form an accepting path
            final ImmutableIntList possibleLastStep = stepIndicators[length];
            for (State nonAcceptState : dfa.nonAcceptStates()) {
                final int finalStepBeNonAccept = possibleLastStep.get(stateEncoder.get(nonAcceptState));
                solver.setLiteralFalsy(finalStepBeNonAccept);
            }
        }
    }
}
