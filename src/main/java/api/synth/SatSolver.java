package api.synth;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;

import java.util.Optional;

public interface SatSolver
{
    int MAX_VARIABLE_NUMBER = 1000000;
    int MAX_CLAUSE_NUMBER = 1000000; // TODO: [tuning] see if there's any effects of modifying this

    boolean isVerbose();

    void setVerbose(boolean value);

    int getTimeoutInSec();

    void setTimeoutInSec(int s);

    long getTimeoutInMs();

    void setTimeoutInMs(long ms);

    ImmutableIntList newFreeVariables(int howMany);

    default void setLiteralTruthy(int literal)
    {
        addClause(literal);
    }

    default void setLiteralsTruthy(int... literals)
    {
        for (int literal : literals) {
            setLiteralTruthy(literal);
        }
    }

    default void setLiteralFalsy(int literal)
    {
        addClause(-literal);
    }

    default void setLiteralsFalsy(int... literals)
    {
        for (int literal : literals) {
            setLiteralFalsy(literal);
        }
    }

    default void markAsEquivalent(int literal1, int literal2)
    {
        addClause(-literal1, literal2);
        addClause(literal1, -literal2);
    }

    default void markEachAsEquivalent(int... literals)
    {
        for (int i = 0; i < literals.length - 1; i++) {
            addClause(-literals[i], literals[i + 1]);
        }
        addClause(-literals[literals.length - 1], literals[0]);
    }

    /**
     * Behaves the same as {@link #markEachAsEquivalent(int...)}.
     */
    default void syncLiterals(int... literals)
    {
        markEachAsEquivalent(literals);
    }

    /**
     * Adds a DIMACS-CNF format clause to the problem.
     *
     * @param clause the clause to be added
     */
    void addClause(int... clause);

    /**
     * Behaves the same as {@link #addClause(int...)}, but accepting an
     * {@link ImmutableIntList} as the given clause.
     */
    default void addClause(ImmutableIntList clause)
    {
        addClause(clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClause(ImmutableIntList)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the clause
     * @param clause    the clause to be added
     */
    default void addClauseIf(int indicator, ImmutableIntList clause)
    {
        MutableIntList clauseAsList = clause.toList();
        clauseAsList.add(-indicator);
        addClause(clauseAsList.toArray());
    }

    /**
     * Ensures the given clause having at least the number ({@code degree}) of
     * the literals being assigned true in the model.
     *
     * @param degree the lower bound of the number of the true literals
     * @param clause the clause to be added
     */
    void addClauseAtLeast(int degree, int... clause);

    /**
     * Behaves the same as {@link #addClauseAtLeast(int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    default void addClauseAtLeast(int degree, ImmutableIntList clause)
    {
        addClauseAtLeast(degree, clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClauseAtLeast(int, int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the at-least clause
     * @param degree    the lower bound of the number of the true literals
     * @param clause    the clause to be added
     */
    default void addClauseAtLeastIf(int indicator, int degree, int... clause)
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

    /**
     * Behaves the same as {@link #addClauseAtLeastIf(int, int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    default void addClauseAtLeastIf(int indicator, int degree, ImmutableIntList clause)
    {
        addClauseAtLeastIf(indicator, degree, clause.toArray());
    }

    /**
     * Ensures the given clause having at most the number ({@code degree}) of
     * the literals being assigned true in the model.
     *
     * @param degree the upper bound of the number of the true literals
     * @param clause the clause to be added
     */
    void addClauseAtMost(int degree, int... clause);

    /**
     * Behaves the same as {@link #addClauseAtMost(int, int...)}, but accepting
     * an {@link ImmutableIntList} as the given clause.
     */
    default void addClauseAtMost(int degree, ImmutableIntList clause)
    {
        addClauseAtMost(degree, clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClauseAtMost(int, int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the at-most clause
     * @param degree    the upper bound of the number of the true literals
     * @param clause    the clause to be added
     */
    default void addClauseAtMostIf(int indicator, int degree, int... clause)
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

    /**
     * Behaves the same as {@link #addClauseAtMostIf(int, int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    default void addClauseAtMostIf(int indicator, int degree, ImmutableIntList clause)
    {
        addClauseAtMostIf(indicator, degree, clause.toArray());
    }

    /**
     * Ensures exactly the number ({@code degree}) of the given literals being
     * assigned true in the model.
     *
     * @param degree the exact number the true literals
     * @param clause the clause to be added
     */
    void addClauseExactly(int degree, int... clause);

    /**
     * Behaves the same as {@link #addClauseExactly(int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    default void addClauseExactly(int degree, ImmutableIntList clause)
    {
        addClauseExactly(degree, clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClauseExactly(int, int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the exact clause
     * @param degree    the exact number the true literals
     * @param clause    the clause to be added
     */
    default void addClauseExactlyIf(int indicator, int degree, int... clause)
    {
        addClauseAtLeastIf(indicator, degree, clause);
        addClauseAtMostIf(indicator, degree, clause);
    }

    /**
     * Behaves the same as {@link #addClauseExactlyIf(int, int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    default void addClauseExactlyIf(int indicator, int degree, ImmutableIntList clause)
    {
        addClauseExactlyIf(indicator, degree, clause.toArray());
    }

    /**
     * Prevents the given clause (with all its literals assigned true) showing
     * up as an model (or as an subset of the model).
     *
     * @param clause the clause to be blocked
     */
    default void addClauseBlocking(int... clause)
    {
        final int[] blockingClause = new int[clause.length];
        for (int i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        addClause(blockingClause);
    }

    /**
     * Behaves the same as {@link #addClauseBlocking(int...)}, but accepting an
     * {@link ImmutableIntList} as the given clause.
     */
    default void addClauseBlockingIf(int indicator, int... clause)
    {
        final int[] blockingClause = new int[clause.length + 1];
        for (int i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        blockingClause[clause.length] = -indicator;
        addClause(blockingClause);
    }

    /**
     * Adds the given implication as a clause.
     *
     * @param antecedent the antecedent of the implication
     * @param consequent the consequent of the implication
     */
    default void addImplication(int antecedent, int consequent)
    {
        addClause(-antecedent, consequent);
    }

    /**
     * Behaves the same as {@link #addImplication(int, int)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator  whether to activate the implication clause
     * @param antecedent the antecedent of the implication
     * @param consequent the consequent of the implication
     */
    default void addImplicationIf(int indicator, int antecedent, int consequent)
    {
        addClause(-indicator, -antecedent, consequent);
    }

    /**
     * Behaves the same as {@link #addImplication(int, int)}, but accepting
     * multiple consequents.
     */
    default void addImplications(int antecedent, int... consequents)
    {
        for (int consequent : consequents) {
            addImplication(antecedent, consequent);
        }
    }

    /**
     * Behaves the same as {@link #addImplicationIf(int, int, int)}, but
     * accepting multiple consequents.
     */
    default void addImplicationsIf(int indicator, int antecedent, int... consequents)
    {
        for (int consequent : consequents) {
            addImplicationIf(indicator, antecedent, consequent);
        }
    }

    Boolean findItSatisfiable();

    default ImmutableIntSet findModel()
    {
        final Boolean satisfiable = findItSatisfiable();
        if (satisfiable == null || !satisfiable) {
            return null;
        }
        return getModel();
    }

    ImmutableIntSet getModel();

    ImmutableIntSet getModelTruthyVariables();

    ImmutableIntSet getModelFalsyVariables();

    void reset();
}
