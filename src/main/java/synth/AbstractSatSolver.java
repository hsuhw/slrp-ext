package synth;

import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.list.primitive.IntInterval;

import java.util.Optional;

public abstract class AbstractSatSolver implements SatSolver
{
    @Override
    public abstract boolean isVerbose();

    @Override
    public abstract void setVerbose(boolean v);

    @Override
    public abstract int getTimeoutInSec();

    @Override
    public abstract void setTimeoutInSec(int t);

    @Override
    public abstract long getTimeoutInMs();

    @Override
    public abstract void setTimeoutInMs(long t);

    @Override
    public abstract IntInterval newFreeVariables(int howMany);

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

    @Override
    public abstract void addClause(int... clause);

    @Override
    public void addClause(IntInterval clause)
    {
        addClause(clause.toArray());
    }

    @Override
    public void addClauseIf(int indicator, IntInterval clause)
    {
        MutableIntList clauseAsList = clause.toList();
        clauseAsList.add(-indicator);
        addClause(clauseAsList.toArray());
    }

    @Override
    public abstract void addClauseAtLeast(int degree, int... clause);

    @Override
    public void addClauseAtLeast(int degree, IntInterval clause)
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
    public void addClauseAtLeastIf(int indicator, int degree, IntInterval clause)
    {
        addClauseAtLeastIf(indicator, degree, clause.toArray());
    }

    @Override
    public abstract void addClauseAtMost(int degree, int... clause);

    @Override
    public void addClauseAtMost(int degree, IntInterval clause)
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
    public void addClauseAtMostIf(int indicator, int degree, IntInterval clause)
    {
        addClauseAtMostIf(indicator, degree, clause.toArray());
    }

    @Override
    public abstract void addClauseExactly(int degree, int... clause);

    @Override
    public void addClauseExactly(int degree, IntInterval clause)
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
    public void addClauseExactlyIf(int indicator, int degree, IntInterval clause)
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
    public abstract Optional<Boolean> findItSatisfiable();

    @Override
    public Optional<ImmutableIntSet> findModel()
    {
        return findItSatisfiable().map(found -> found ? getModel() : null);
    }

    @Override
    public abstract ImmutableIntSet getModel();

    @Override
    public abstract ImmutableIntSet getModelTruthyVariables();

    @Override
    public abstract ImmutableIntSet getModelFalsyVariables();

    @Override
    public abstract void reset();
}
