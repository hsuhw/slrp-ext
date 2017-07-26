package synth;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.impl.list.primitive.IntInterval;

class BasicFSAEncoding implements FSAEncoding
{
    private static final int DEFAULT_START_STATE_INDEX = 0;
    private static final int DEFAULT_EPSILON_SYMBOL_INDEX = 0;

    private final SatSolver solver;
    private final int stateNumber;
    private final int alphabetSize;
    private final int startStateIndex;
    private final int epsilonSymbolIndex;
    private IntInterval[][] transitionIndicators;
    private IntInterval acceptStateIndicators;
    private boolean determinismEnsured;
    private boolean noDanglingStatesEnsured;
    private boolean noDeadEndStatesEnsured;

    private void prepareTransitionIndicators()
    {
        transitionIndicators = new IntInterval[stateNumber][alphabetSize];
        for (int state = 0; state < stateNumber; state++) {
            for (int symbol = 0; symbol < alphabetSize; symbol++) {
                transitionIndicators[state][symbol] = solver.newFreeVariables(stateNumber);
            }
        }
    }

    private void prepareAcceptStateIndicators()
    {
        acceptStateIndicators = solver.newFreeVariables(stateNumber);
        solver.addClause(acceptStateIndicators); // at least one accept state
    }

    BasicFSAEncoding(SatSolver solver, int stateNumber, int alphabetSize)
    {
        this(solver, stateNumber, alphabetSize, DEFAULT_START_STATE_INDEX, DEFAULT_EPSILON_SYMBOL_INDEX);
    }

    BasicFSAEncoding(SatSolver solver, int stateNumber, int alphabetSize, int startStateIndex, int epsilonSymbolIndex)
    {
        this.solver = solver;
        this.stateNumber = stateNumber;
        this.alphabetSize = alphabetSize;
        this.startStateIndex = startStateIndex;
        this.epsilonSymbolIndex = epsilonSymbolIndex;

        prepareTransitionIndicators();
        prepareAcceptStateIndicators();
    }

    private void ensureDWithDefaultIndexes()
    {
        for (int dept = 0; dept < stateNumber; dept++) {
            transitionIndicators[dept][0].forEach(solver::setLiteralFalsy);
            for (int symbol = 1; symbol < alphabetSize; symbol++) {
                final IntInterval takableDest = transitionIndicators[dept][symbol];
                solver.addClauseAtMost(1, takableDest);
            }
        }
    }

    @Override
    public void ensureDeterminism()
    {
        if (determinismEnsured) {
            return;
        }

        if (epsilonSymbolIndex == DEFAULT_EPSILON_SYMBOL_INDEX) {
            ensureDWithDefaultIndexes();
            determinismEnsured = true;
            return;
        }

        for (int dept = 0; dept < stateNumber; dept++) {
            for (int symbol = 0; symbol < alphabetSize; symbol++) {
                if (symbol == epsilonSymbolIndex) {
                    transitionIndicators[dept][symbol].forEach(solver::setLiteralFalsy);
                } else {
                    final IntInterval takableDest = transitionIndicators[dept][symbol];
                    solver.addClauseAtMost(1, takableDest);
                }
            }
        }
        determinismEnsured = true;
    }

    private IntInterval[] prepareDistanceIndicators()
    {
        final IntInterval[] distanceIndicators = new IntInterval[stateNumber];
        for (int state = 0; state < stateNumber; state++) {
            final IntInterval possibleDistance = solver.newFreeVariables(stateNumber);
            distanceIndicators[state] = possibleDistance; // from 0 to n - 1
            solver.addClauseAtMost(1, possibleDistance);
        }
        return distanceIndicators;
    }

    private void encodePossibleDistByTrans(int prev, int curr, int sym, IntInterval[] distanceIndicators)
    {
        final IntInterval possibleDistByGivenTrans = solver.newFreeVariables(stateNumber);
        solver.addClauseAtMost(1, possibleDistByGivenTrans);
        final int transCanCauseNoDist = possibleDistByGivenTrans.get(0);
        solver.setLiteralFalsy(transCanCauseNoDist);
        for (int distNum = 1; distNum < stateNumber; distNum++) {
            final int ifTransCauseDistNum = possibleDistByGivenTrans.get(distNum);
            final int transBeAvailable = transitionIndicators[prev][sym].get(curr);
            final int prevBeNumMinusOne = distanceIndicators[prev].get(distNum - 1);
            final int currBeNum = distanceIndicators[curr].get(distNum);

            // ifTransCauseDistNum <--> transBeAvailable && prevBeNumMinusOne && currBeNum
            solver.addImplications(ifTransCauseDistNum, transBeAvailable, prevBeNumMinusOne, currBeNum);
            solver.addClause(-transBeAvailable, -prevBeNumMinusOne, -currBeNum, ifTransCauseDistNum);
        }
    }

    private void ensureNDSWithDefaultIndexes(IntInterval[] distFromStartIndicators)
    {
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 1; qj < stateNumber; qj++) {
                for (int symbol = 1; symbol < alphabetSize; symbol++) {
                    encodePossibleDistByTrans(qi, qj, symbol, distFromStartIndicators);
                }
            }
        }
    }

    @Override
    public void ensureNoDanglingStates()
    {
        if (noDanglingStatesEnsured) {
            return;
        }

        final IntInterval[] distFromStartIndicators = prepareDistanceIndicators();
        final int startStateDistBeZero = distFromStartIndicators[startStateIndex].get(0);
        solver.setLiteralTruthy(startStateDistBeZero);

        if (startStateIndex == DEFAULT_START_STATE_INDEX && epsilonSymbolIndex == DEFAULT_EPSILON_SYMBOL_INDEX) {
            ensureNDSWithDefaultIndexes(distFromStartIndicators);
            noDanglingStatesEnsured = true;
            return;
        }

        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                if (qj == startStateIndex) {
                    continue;
                }
                for (int symbol = 0; symbol < alphabetSize; symbol++) {
                    if (symbol == epsilonSymbolIndex) {
                        continue;
                    }
                    encodePossibleDistByTrans(qi, qj, symbol, distFromStartIndicators);
                }
            }
        }
        noDanglingStatesEnsured = true;
    }

    private void ensureNDESWithDefaultIndexes(IntInterval[] distFromAcceptIndicators)
    {
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int symbol = 1; symbol < alphabetSize; symbol++) {
                    encodePossibleDistByTrans(qi, qj, symbol, distFromAcceptIndicators);
                }
            }
        }
    }

    @Override
    public void ensureNoDeadEndStates()
    {
        if (noDeadEndStatesEnsured) {
            return;
        }

        IntInterval[] distFromAcceptIndicators = prepareDistanceIndicators();
        for (int state = 0; state < stateNumber; state++) {
            final int takenAsAcceptState = acceptStateIndicators.get(state);
            final int distBeZero = distFromAcceptIndicators[state].get(0);
            solver.markAsEquivalent(takenAsAcceptState, distBeZero);
        }

        if (epsilonSymbolIndex == DEFAULT_EPSILON_SYMBOL_INDEX) {
            ensureNDESWithDefaultIndexes(distFromAcceptIndicators);
            noDeadEndStatesEnsured = true;
            return;
        }

        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int symbol = 0; symbol < alphabetSize; symbol++) {
                    if (symbol == epsilonSymbolIndex) {
                        continue;
                    }
                    encodePossibleDistByTrans(qi, qj, symbol, distFromAcceptIndicators);
                }
            }
        }
        noDeadEndStatesEnsured = true;
    }

    private IntInterval[] encodeEachPossibleStepsOnInputRead(int activated, ImmutableIntList word)
    {
        // define each possible step over states on input consuming
        final IntInterval[] stepIndicators = new IntInterval[word.size() + 1];
        for (int readHead = 0; readHead < word.size() + 1; readHead++) {
            final IntInterval possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            solver.addClauseExactlyIf(activated, 1, possibleStateStepping);
        }
        final int initialStepBeStartState = stepIndicators[0].get(startStateIndex);
        solver.addImplication(activated, initialStepBeStartState);

        // make the taken steps to represent the given word
        for (int readHead = 0; readHead < word.size(); readHead++) {
            for (int qi = 0; qi < stateNumber; qi++) {
                for (int qj = 0; qj < stateNumber; qj++) {
                    final int takenQiAsCurrentStep = stepIndicators[readHead].get(qi);
                    final int takenQjAsNextStep = stepIndicators[readHead + 1].get(qj);
                    final int symbol = word.get(readHead);
                    final int transBeAvailable = transitionIndicators[qi][symbol].get(qj);
                    solver.addClause(-activated, -takenQiAsCurrentStep, -takenQjAsNextStep, transBeAvailable);
                }
            }
        }

        return stepIndicators;
    }

    private void ensureAcceptWordIf(int activated, ImmutableIntList word)
    {
        final IntInterval[] stepIndicators = encodeEachPossibleStepsOnInputRead(activated, word);

        // make the taken steps form an accepting path
        final IntInterval takableLastStep = stepIndicators[word.size()];
        for (int state = 0; state < stateNumber; state++) {
            final int ifTakenAsLastStep = takableLastStep.get(state);
            final int beAcceptState = acceptStateIndicators.get(state);
            solver.addImplicationIf(activated, ifTakenAsLastStep, beAcceptState);
        }
    }

    @Override
    public void ensureAcceptWord(ImmutableIntList word)
    {
        final int activated = solver.newFreeVariables(1).getFirst();
        ensureAcceptWordIf(activated, word);
        solver.setLiteralTruthy(activated);
    }

    private void ensureNotAcceptWordIf(int activated, ImmutableIntList word)
    {
        final IntInterval[] stepIndicators = encodeEachPossibleStepsOnInputRead(activated, word);

        // make the taken steps form an non-accepting path
        final IntInterval takableLastStep = stepIndicators[word.size()];
        for (int state = 0; state < stateNumber; state++) {
            final int ifTakenAsLastStep = takableLastStep.get(state);
            final int beNonAcceptState = -acceptStateIndicators.get(state);
            solver.addImplicationIf(activated, ifTakenAsLastStep, beNonAcceptState);
        }
    }

    @Override
    public void ensureNotAcceptWord(ImmutableIntList word)
    {
        final int activated = solver.newFreeVariables(1).getFirst();
        ensureNotAcceptWordIf(activated, word);
        solver.setLiteralTruthy(activated);
    }

    @Override
    public void whetherAcceptWord(int indicator, ImmutableIntList word)
    {
        ensureAcceptWordIf(indicator, word);
        ensureNotAcceptWordIf(-indicator, word);
    }
}
