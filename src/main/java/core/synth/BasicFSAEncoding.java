package core.synth;

import api.automata.IntAlphabetTranslator;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import api.synth.FSAEncoding;
import api.synth.SatSolver;
import core.automata.Alphabet;
import core.automata.State;
import core.automata.fsa.BasicFSABuilder;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;

public class BasicFSAEncoding<S extends Symbol> implements FSAEncoding<S>
{
    private static final int START_STATE_INDEX = 0;

    private final SatSolver solver;
    private final int stateNumber;
    private final IntAlphabetTranslator<S> alphabetEncoding;
    private ImmutableIntList[][] transitionIndicators;
    private ImmutableIntList acceptStateIndicators;
    private boolean determinismEnsured;
    private boolean noDanglingStatesEnsured;
    private boolean noDeadEndStatesEnsured;

    private void prepareTransitionIndicators()
    {
        transitionIndicators = new ImmutableIntList[stateNumber][alphabetEncoding.size()];
        for (int state = 0; state < stateNumber; state++) {
            for (int symbol = 0; symbol < alphabetEncoding.size(); symbol++) {
                transitionIndicators[state][symbol] = solver.newFreeVariables(stateNumber);
            }
        }
    }

    private void prepareAcceptStateIndicators()
    {
        acceptStateIndicators = solver.newFreeVariables(stateNumber);
        solver.addClause(acceptStateIndicators); // at least one accept state
    }

    public BasicFSAEncoding(SatSolver solver, int stateNumber, IntAlphabetTranslator<S> alphabetEncoding)
    {
        this.solver = solver;
        this.stateNumber = stateNumber;
        this.alphabetEncoding = alphabetEncoding;

        prepareTransitionIndicators();
        prepareAcceptStateIndicators();
    }

    @Override
    public void ensureDeterminism()
    {
        if (determinismEnsured) {
            return;
        }
        for (int dept = 0; dept < stateNumber; dept++) {
            transitionIndicators[dept][0].forEach(solver::setLiteralFalsy);
            for (int symbol = 1; symbol < alphabetEncoding.size(); symbol++) {
                final ImmutableIntList possibleDest = transitionIndicators[dept][symbol];
                solver.addClauseAtMost(1, possibleDest); // can be zero
            }
        }
        determinismEnsured = true;
    }

    private ImmutableIntList[] prepareDistanceIndicators()
    {
        final ImmutableIntList[] distanceIndicators = new ImmutableIntList[stateNumber];
        for (int state = 0; state < stateNumber; state++) {
            final ImmutableIntList possibleDistance = solver.newFreeVariables(stateNumber);
            distanceIndicators[state] = possibleDistance; // from 0 to n - 1
            solver.addClause(possibleDistance);
        }
        return distanceIndicators;
    }

    private void encodePossibleDistByTrans(int prev, int curr, int sym, ImmutableIntList[] distanceIndicators)
    {
        final ImmutableIntList possibleDistByGivenTrans = solver.newFreeVariables(stateNumber);
        final int transCanCauseNoDist = possibleDistByGivenTrans.get(0);
        solver.setLiteralFalsy(transCanCauseNoDist);
        for (int distNum = 1; distNum < stateNumber; distNum++) {
            final int transCauseDistNum = possibleDistByGivenTrans.get(distNum);
            final int transBeAvailable = transitionIndicators[prev][sym].get(curr);
            final int prevBeNumMinusOne = distanceIndicators[prev].get(distNum - 1);
            final int currBeNum = distanceIndicators[curr].get(distNum);

            // transCauseDistNum <--> transBeAvailable && prevBeNumMinusOne && currBeNum
            solver.addImplications(transCauseDistNum, transBeAvailable, prevBeNumMinusOne, currBeNum);
            solver.addClause(-transBeAvailable, -prevBeNumMinusOne, -currBeNum, transCauseDistNum);
        }
    }

    @Override
    public void ensureNoDanglingStates()
    {
        if (noDanglingStatesEnsured) {
            return;
        }
        final ImmutableIntList[] distFromStartIndicators = prepareDistanceIndicators();
        final int startStateDistBeZero = distFromStartIndicators[START_STATE_INDEX].get(0);
        solver.setLiteralTruthy(startStateDistBeZero);
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 1; qj < stateNumber; qj++) {
                for (int symbol = 1; symbol < alphabetEncoding.size(); symbol++) {
                    encodePossibleDistByTrans(qi, qj, symbol, distFromStartIndicators);
                }
            }
        }
        noDanglingStatesEnsured = true;
    }

    @Override
    public void ensureNoDeadEndStates()
    {
        if (noDeadEndStatesEnsured) {
            return;
        }
        ImmutableIntList[] distFromAcceptIndicators = prepareDistanceIndicators();
        for (int qi = 0; qi < stateNumber; qi++) {
            final int takenAsAcceptState = acceptStateIndicators.get(qi);
            final int distBeZero = distFromAcceptIndicators[qi].get(0);
            solver.markAsEquivalent(takenAsAcceptState, distBeZero);
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int symbol = 1; symbol < alphabetEncoding.size(); symbol++) {
                    encodePossibleDistByTrans(qi, qj, symbol, distFromAcceptIndicators);
                }
            }
        }
        noDeadEndStatesEnsured = true;
    }

    private void ensureAcceptWordIf(int activated, ImmutableIntList word)
    {
        // define each possible step over states on each input symbol read
        final ImmutableIntList[] stepIndicators = new ImmutableIntList[word.size() + 1];
        for (int readHead = 0; readHead < word.size() + 1; readHead++) {
            final ImmutableIntList possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            solver.addClauseIf(activated, possibleStateStepping);
        }
        final int initialStepBeStartState = stepIndicators[0].get(START_STATE_INDEX);
        solver.addImplication(activated, initialStepBeStartState);

        // make the taken steps to represent the given word
        for (int readHead = 0; readHead < word.size(); readHead++) {
            final int symbol = word.get(readHead);
            for (int qi = 0; qi < stateNumber; qi++) {
                final int takenQiAsCurrStep = stepIndicators[readHead].get(qi);
                for (int qj = 0; qj < stateNumber; qj++) {
                    final int takenQjAsNextStep = stepIndicators[readHead + 1].get(qj);
                    final int transBeAvailable = transitionIndicators[qi][symbol].get(qj);
                    solver.addClause(-activated, -takenQiAsCurrStep, -takenQjAsNextStep, transBeAvailable);
                }
            }
        }

        // make the taken steps form an accepting path
        final ImmutableIntList possibleLastStep = stepIndicators[word.size()];
        for (int state = 0; state < stateNumber; state++) {
            final int ifTakenAsLastStep = possibleLastStep.get(state);
            final int beAcceptState = acceptStateIndicators.get(state);
            solver.addImplicationIf(activated, ifTakenAsLastStep, beAcceptState);
        }
    }

    @Override
    public void ensureAcceptingWord(ImmutableList<S> word)
    {
        final ImmutableIntList encodedWord = alphabetEncoding.translate(word);
        final int activated = solver.newFreeVariables(1).getFirst();
        ensureAcceptWordIf(activated, encodedWord);
        solver.setLiteralTruthy(activated);
    }

    private void ensureNotAcceptWordIf(int activated, ImmutableIntList word)
    {
        // prepare the failed-already indicators for each step of the input read
        final ImmutableIntList failedAlreadyIndicators = solver.newFreeVariables(word.size() + 1);
        final int initialStepNeverFailedAlready = -failedAlreadyIndicators.get(0);
        solver.addImplication(activated, initialStepNeverFailedAlready);

        // define each possible step over states on each input symbol read
        final ImmutableIntList[] stepIndicators = new ImmutableIntList[word.size() + 1];
        for (int readHead = 0; readHead < word.size() + 1; readHead++) {
            final ImmutableIntList possibleStateStepping = solver.newFreeVariables(stateNumber);
            stepIndicators[readHead] = possibleStateStepping;
            solver.addClauseIf(activated, possibleStateStepping);
        }
        final int initialStepBeStartState = stepIndicators[0].get(START_STATE_INDEX);
        solver.addImplication(activated, initialStepBeStartState);

        // make the taken steps form an non-accepting path
        for (int readHead = 0; readHead < word.size(); readHead++) {
            final int alreadyFailed = failedAlreadyIndicators.get(readHead);
            final int stillNotFailedAtNext = -failedAlreadyIndicators.get(readHead + 1);
            final int symbol = word.get(readHead);
            for (int qi = 0; qi < stateNumber; qi++) {
                final int takenQiAsCurrStep = stepIndicators[readHead].get(qi);
                for (int qj = 0; qj < stateNumber; qj++) {
                    final int takenQjAsNextStep = stepIndicators[readHead + 1].get(qj);
                    final int transAvailable = transitionIndicators[qi][symbol].get(qj);
                    solver.addClause(-activated, alreadyFailed, -takenQiAsCurrStep, -transAvailable,
                                     stillNotFailedAtNext);
                    solver.addImplicationIf(activated, transAvailable, takenQjAsNextStep);
                }
            }
        }
        final int alreadyFailed = failedAlreadyIndicators.get(word.size());
        final ImmutableIntList possibleLastStep = stepIndicators[word.size()];
        for (int state = 0; state < stateNumber; state++) {
            final int takenAsLastStep = possibleLastStep.get(state);
            final int notAcceptState = -acceptStateIndicators.get(state);
            solver.addClause(-activated, alreadyFailed, -takenAsLastStep, notAcceptState);
        }
    }

    @Override
    public void ensureNotAcceptingWord(ImmutableList<S> word)
    {
        final ImmutableIntList encodedWord = alphabetEncoding.translate(word);
        final int activated = solver.newFreeVariables(1).getFirst();
        ensureNotAcceptWordIf(activated, encodedWord);
        solver.setLiteralTruthy(activated);
    }

    @Override
    public void whetherAcceptWord(int indicator, ImmutableList<S> word)
    {
        final ImmutableIntList encodedWord = alphabetEncoding.translate(word);
        ensureAcceptWordIf(indicator, encodedWord);
        ensureNotAcceptWordIf(-indicator, encodedWord);
    }

    @Override
    public void ensureNoWordsPurelyMadeOf(ImmutableSet<S> symbols)
    {
        final ImmutableIntList encodedSymbols = alphabetEncoding.translate(symbols.toList().toImmutable());

        final ImmutableIntList canBePurelyMadeUntil = solver.newFreeVariables(stateNumber);
        final int startStateAlwaysPossible = canBePurelyMadeUntil.get(START_STATE_INDEX);
        solver.setLiteralTruthy(startStateAlwaysPossible);

        for (int qi = 0; qi < stateNumber; qi++) {
            final int purityAlreadyBroken = -canBePurelyMadeUntil.get(qi);
            final int ifIsAcceptState = acceptStateIndicators.get(qi);
            solver.addImplication(ifIsAcceptState, purityAlreadyBroken);
            for (int qj = 0; qj < stateNumber; qj++) {
                final int stillPossibleAtQj = canBePurelyMadeUntil.get(qj);
                for (int s = 0; s < encodedSymbols.size(); s++) {
                    final int transAvailable = transitionIndicators[qi][encodedSymbols.get(s)].get(qj);
                    solver.addClause(purityAlreadyBroken, -transAvailable, stillPossibleAtQj);
                }
            }
        }
    }

    @Override
    public void blockCurrentSolution()
    {
        // get solution (if any)
        if (!solver.findItSatisfiable()) {
            return;
        }
        final ImmutableIntSet truthyIndicators = solver.getModelTruthyVariables();

        // collect which to be blocked
        final MutableIntSet trackedIndicators = new IntHashSet(truthyIndicators.size());
        acceptStateIndicators.forEach(acceptIndicator -> {
            if (truthyIndicators.contains(acceptIndicator)) {
                trackedIndicators.add(acceptIndicator);
            }
        });
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int s = 0; s < alphabetEncoding.size(); s++) {
                    final int transIndicator = transitionIndicators[qi][s].get(qj);
                    if (truthyIndicators.contains(transIndicator)) {
                        trackedIndicators.add(transIndicator);
                    }
                }
            }
        }

        // block the solution
        solver.addClauseBlocking(trackedIndicators.toArray());
    }

    @Override
    public FSA<S> toFSA()
    {
        // get solution (if any)
        if (!solver.findItSatisfiable()) {
            return null;
        }
        final ImmutableIntSet truthyIndicators = solver.getModelTruthyVariables();

        // prepare FSA builder
        final int symbolNumber = alphabetEncoding.size();
        final S epsilonSymbol = alphabetEncoding.getOriginEpsilonSymbol();
        final FSA.Builder<S> builder = new BasicFSABuilder<>(symbolNumber, epsilonSymbol, stateNumber);

        // prepare state objects
        final State[] states = new State[stateNumber];
        for (int i = 0; i < stateNumber; i++) {
            states[i] = new State("s" + i);
        }

        // decode start & accept states
        builder.addStartState(states[START_STATE_INDEX]);
        acceptStateIndicators.forEachWithIndex((i, s) -> {
            if (truthyIndicators.contains(i)) {
                builder.addAcceptState(states[s]);
            }
        });

        // decode transitions
        for (int qi = 0; qi < stateNumber; qi++) {
            for (int qj = 0; qj < stateNumber; qj++) {
                for (int s = 0; s < alphabetEncoding.size(); s++) {
                    if (truthyIndicators.contains(transitionIndicators[qi][s].get(qj))) {
                        builder.addTransition(states[qi], states[qj], alphabetEncoding.originSymbolOf(s));
                    }
                }
            }
        }

        // build
        final Alphabet<S> originalAlphabet = new Alphabet<>(alphabetEncoding.getOriginAlphabet(),
                                                            alphabetEncoding.getOriginEpsilonSymbol());
        return builder.settleRecords(originalAlphabet);
    }
}
