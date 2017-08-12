package core.synth;

import api.synth.SatSolver;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;

public abstract class AbstractSatSolver implements SatSolver
{
    @Override
    public void setLiteralTruthy(int literal)
    {
        addClause(literal);
    }

    @Override
    public void setLiteralsTruthy(int... literals)
    {
        for (int literal : literals) {
            setLiteralTruthy(literal);
        }
    }

    @Override
    public void setLiteralFalsy(int literal)
    {
        addClause(-literal);
    }

    @Override
    public void setLiteralsFalsy(int... literals)
    {
        for (int literal : literals) {
            setLiteralFalsy(literal);
        }
    }

    @Override
    public void markAsEquivalent(int literal1, int literal2)
    {
        addClause(-literal1, literal2);
        addClause(literal1, -literal2);
    }

    @Override
    public void markEachAsEquivalent(int... literals)
    {
        for (int i = 0; i < literals.length - 1; i++) {
            addClause(-literals[i], literals[i + 1]);
        }
        addClause(-literals[literals.length - 1], literals[0]);
    }

    @Override
    public void syncLiterals(int... literals)
    {
        markEachAsEquivalent(literals);
    }

    private void encodeGreaterEqualThanAtCurrentDigit(int alreadyGreater, int greaterHere, int digit1, int digit2)
    {
        // -alreadyGreater --> (greater here) || (equal here)
        addImplicationIf(-alreadyGreater, digit2, digit1);

        // greaterHere <-- alreadyGreater || (digit1 && -digit2)
        addImplication(alreadyGreater, greaterHere);
        addClause(-digit1, digit2, greaterHere);
        // greaterHere --> alreadyGreater || (digit1 && -digit2)
        addClauseIf(greaterHere, alreadyGreater, digit1);
        addClauseIf(greaterHere, alreadyGreater, -digit2);
    }

    @Override
    public void markAsGreaterEqualThan(int[] bitArray1, int[] bitArray2)
    {
        if (bitArray1.length == 0 || bitArray2.length == 0) {
            throw new IllegalArgumentException("zero length arrays are given");
        }
        final int deltaLength = bitArray1.length - bitArray2.length;
        final int commonDigitLength = Math.min(bitArray1.length, bitArray2.length);
        final ImmutableIntList greaterAlreadyIndicators = newFreeVariables(commonDigitLength + 1);
        if (deltaLength > 0) { // array1 being longer

            // define the greater situation at longer part
            final int[] longerPartHasValue = new int[deltaLength];
            System.arraycopy(bitArray1, 0, longerPartHasValue, 0, deltaLength);
            final int greaterAtLongerPart = greaterAlreadyIndicators.get(0);
            // longerPartHasValue <--> greaterAtLongerPart
            addClauseIf(greaterAtLongerPart, longerPartHasValue);
            for (int i = 0; i < deltaLength; i++) {
                addImplication(bitArray1[i], greaterAtLongerPart);
            }

            // define the greater situation at common part
            for (int i = 0; i < bitArray2.length; i++) {
                final int alreadyGreater = greaterAlreadyIndicators.get(i);
                final int greaterHere = greaterAlreadyIndicators.get(i + 1);
                final int digit1 = bitArray1[deltaLength + i];
                final int digit2 = bitArray2[i];
                encodeGreaterEqualThanAtCurrentDigit(alreadyGreater, greaterHere, digit1, digit2);
            }
        } else {
            final int absDeltaLength = -deltaLength;

            // define the greater situation at longer part
            setLiteralFalsy(greaterAlreadyIndicators.get(0));
            for (int i = 0; i < absDeltaLength; i++) {
                setLiteralFalsy(bitArray2[i]);
            }

            // define the greater situation at common part
            for (int i = 0; i < bitArray1.length; i++) {
                final int alreadyGreater = greaterAlreadyIndicators.get(i);
                final int greaterHere = greaterAlreadyIndicators.get(i + 1);
                final int digit1 = bitArray1[i];
                final int digit2 = bitArray2[absDeltaLength + i];
                encodeGreaterEqualThanAtCurrentDigit(alreadyGreater, greaterHere, digit1, digit2);
            }
        }
    }

    @Override
    public void markAsGreaterEqualThan(ImmutableIntList bitArray1, ImmutableIntList bitArray2)
    {
        markAsGreaterEqualThan(bitArray1.toArray(), bitArray2.toArray());
    }

    @Override
    public void addClause(ImmutableIntList clause)
    {
        addClause(clause.toArray());
    }

    @Override
    public void addClauseIf(int indicator, int... clause)
    {
        MutableIntList clauseAsList = IntLists.mutable.of(clause);
        clauseAsList.add(-indicator);
        addClause(clauseAsList.toArray());
    }

    @Override
    public void addClauseIf(int indicator, ImmutableIntList clause)
    {
        addClauseIf(indicator, clause.toArray());
    }

    @Override
    public void addClauseAtLeast(int degree, ImmutableIntList clause)
    {
        addClauseAtLeast(degree, clause.toArray());
    }

    @Override
    public void addClauseAtLeastIf(int indicator, int degree, int... clause)
    {
        final MutableIntList paddingAsList = newFreeVariables(degree).toList();
        final MutableIntList clauseAsList = IntLists.mutable.of(clause);
        clauseAsList.addAll(paddingAsList);
        final int[] paddedClause = clauseAsList.toArray();
        addClauseAtLeast(degree, paddedClause);

        paddingAsList.add(-indicator);
        final int[] switches = paddingAsList.toArray();
        syncLiterals(switches);
    }

    @Override
    public void addClauseAtLeastIf(int indicator, int degree, ImmutableIntList clause)
    {
        addClauseAtLeastIf(indicator, degree, clause.toArray());
    }

    @Override
    public void addClauseAtMost(int degree, ImmutableIntList clause)
    {
        addClauseAtMost(degree, clause.toArray());
    }

    @Override
    public void addClauseAtMostIf(int indicator, int degree, int... clause)
    {
        final MutableIntList paddingAsList = newFreeVariables(clause.length - degree).toList();
        final MutableIntList clauseAsList = IntLists.mutable.of(clause);
        clauseAsList.addAll(paddingAsList);
        final int[] paddedClause = clauseAsList.toArray();
        addClauseAtMost(clause.length, paddedClause);

        paddingAsList.add(indicator);
        final int[] switches = paddingAsList.toArray();
        syncLiterals(switches);
    }

    @Override
    public void addClauseAtMostIf(int indicator, int degree, ImmutableIntList clause)
    {
        addClauseAtMostIf(indicator, degree, clause.toArray());
    }

    @Override
    public void addClauseExactly(int degree, ImmutableIntList clause)
    {
        addClauseExactly(degree, clause.toArray());
    }

    @Override
    public void addClauseExactlyIf(int indicator, int degree, int... clause)
    {
        addClauseAtLeastIf(indicator, degree, clause);
        addClauseAtMostIf(indicator, degree, clause);
    }

    @Override
    public void addClauseExactlyIf(int indicator, int degree, ImmutableIntList clause)
    {
        addClauseExactlyIf(indicator, degree, clause.toArray());
    }

    @Override
    public void addClauseBlocking(int... clause)
    {
        final int[] blockingClause = new int[clause.length];
        for (int i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        addClause(blockingClause);
    }

    @Override
    public void addClauseBlocking(ImmutableIntSet clause)
    {
        addClauseBlocking(clause.toArray());
    }

    @Override
    public void addClauseBlockingIf(int indicator, int... clause)
    {
        final int[] blockingClause = new int[clause.length + 1];
        for (int i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        blockingClause[clause.length] = -indicator;
        addClause(blockingClause);
    }

    @Override
    public void addClauseBlockingIf(int indicator, ImmutableIntSet clause)
    {
        addClauseBlockingIf(indicator, clause.toArray());
    }

    @Override
    public void addImplication(int antecedent, int consequent)
    {
        addClause(-antecedent, consequent);
    }

    @Override
    public void addImplicationIf(int indicator, int antecedent, int consequent)
    {
        addClause(-indicator, -antecedent, consequent);
    }

    @Override
    public void addImplications(int antecedent, int... consequents)
    {
        for (int consequent : consequents) {
            addImplication(antecedent, consequent);
        }
    }

    @Override
    public void addImplicationsIf(int indicator, int antecedent, int... consequents)
    {
        for (int consequent : consequents) {
            addImplicationIf(indicator, antecedent, consequent);
        }
    }

    @Override
    public ImmutableIntSet findModel()
    {
        final Boolean satisfiable = findItSatisfiable();
        if (satisfiable == null || !satisfiable) {
            return null;
        }
        return getModel();
    }
}
