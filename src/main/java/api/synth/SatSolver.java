package api.synth;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;

/**
 * The API definition for the SAT solver functionalities we use in programs.
 */
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

    void setLiteralTruthy(int literal);

    void setLiteralsTruthy(int... literals);

    void setLiteralFalsy(int literal);

    void setLiteralsFalsy(int... literals);

    void markAsEquivalent(int literal1, int literal2);

    void markEachAsEquivalent(int... literals);

    /**
     * Behaves the same as {@link #markEachAsEquivalent(int...)}.
     */
    void syncLiterals(int... literals);

    /**
     * Makes the first given array of literals, if taken as a bit vector (zero
     * cell as the highiest digit), have binary value greater equal than the
     * second given array.
     */
    void markAsGreaterEqualThan(int[] bitArray1, int[] bitArray2);

    /**
     * Behaves the same as {@link #markAsGreaterEqualThan(int[], int[])}, but
     * accepting {@link ImmutableIntList} as the given bit arrays.
     */
    void markAsGreaterEqualThan(ImmutableIntList bitArray1, ImmutableIntList bitArray2);

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
    void addClause(ImmutableIntList clause);

    /**
     * Behaves the same as {@link #addClause(int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the clause
     * @param clause    the clause to be added
     */
    void addClauseIf(int indicator, int... clause);

    /**
     * Behaves the same as {@link #addClauseIf(int, int...)}, but accepting an
     * {@link ImmutableIntList} as the given clause.
     */
    void addClauseIf(int indicator, ImmutableIntList clause);

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
    void addClauseAtLeast(int degree, ImmutableIntList clause);

    /**
     * Behaves the same as {@link #addClauseAtLeast(int, int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the at-least clause
     * @param degree    the lower bound of the number of the true literals
     * @param clause    the clause to be added
     */
    void addClauseAtLeastIf(int indicator, int degree, int... clause);

    /**
     * Behaves the same as {@link #addClauseAtLeastIf(int, int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    void addClauseAtLeastIf(int indicator, int degree, ImmutableIntList clause);

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
    void addClauseAtMost(int degree, ImmutableIntList clause);

    /**
     * Behaves the same as {@link #addClauseAtMost(int, int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the at-most clause
     * @param degree    the upper bound of the number of the true literals
     * @param clause    the clause to be added
     */
    void addClauseAtMostIf(int indicator, int degree, int... clause);

    /**
     * Behaves the same as {@link #addClauseAtMostIf(int, int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    void addClauseAtMostIf(int indicator, int degree, ImmutableIntList clause);

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
    void addClauseExactly(int degree, ImmutableIntList clause);

    /**
     * Behaves the same as {@link #addClauseExactly(int, int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the exact clause
     * @param degree    the exact number the true literals
     * @param clause    the clause to be added
     */
    void addClauseExactlyIf(int indicator, int degree, int... clause);

    /**
     * Behaves the same as {@link #addClauseExactlyIf(int, int, int...)}, but
     * accepting an {@link ImmutableIntList} as the given clause.
     */
    void addClauseExactlyIf(int indicator, int degree, ImmutableIntList clause);

    /**
     * Prevents the given clause (with all its literals assigned true) showing
     * up as an model (or as an subset of the model).
     *
     * @param clause the clause to be blocked
     */
    void addClauseBlocking(int... clause);

    /**
     * Behaves the same as {@link #addClauseBlocking(int...)}, but accepting an
     * {@link ImmutableIntSet} as the given clause.
     */
    void addClauseBlocking(ImmutableIntSet clause);

    /**
     * Behaves the same as {@link #addClauseBlocking(int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the blocking clause
     * @param clause    the clause to be added
     */
    void addClauseBlockingIf(int indicator, int... clause);

    /**
     * Behaves the same as {@link #addClauseBlockingIf(int, int...)}, but
     * accepting an {@link ImmutableIntSet} as the given clause.
     */
    void addClauseBlockingIf(int indicator, ImmutableIntSet clause);

    /**
     * Adds the given implication as a clause.
     *
     * @param antecedent the antecedent of the implication
     * @param consequent the consequent of the implication
     */
    void addImplication(int antecedent, int consequent);

    /**
     * Behaves the same as {@link #addImplication(int, int)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator  whether to activate the implication clause
     * @param antecedent the antecedent of the implication
     * @param consequent the consequent of the implication
     */
    void addImplicationIf(int indicator, int antecedent, int consequent);

    /**
     * Behaves the same as {@link #addImplication(int, int)}, but accepting
     * multiple consequents.
     */
    void addImplications(int antecedent, int... consequents);

    /**
     * Behaves the same as {@link #addImplicationIf(int, int, int)}, but
     * accepting multiple consequents.
     */
    void addImplicationsIf(int indicator, int antecedent, int... consequents);

    /**
     * Returns {@link Boolean} for whether the given constraints can be
     * satisfied; {@code null} when the solver is not sure.
     */
    Boolean findItSatisfiable();

    /**
     * Determines the satisfiability and returns the model if found any.
     */
    ImmutableIntSet findModel();

    /**
     * Returns the model of the given constraints after the satisfiability has
     * been determined.
     *
     * @return an {@link ImmutableIntSet} containing the true variable IDs in
     * positive {@code int} and the false variable IDs in negative {@code int}
     */
    ImmutableIntSet getModel();

    /**
     * Returns the variables in the model that have been assigned true of the
     * given constraints after the satisfiability has been determined.
     *
     * @return an {@link ImmutableIntSet} containing the true variable IDs
     */
    ImmutableIntSet getModelTruthyVariables();

    /**
     * Returns the variables in the model that have been assigned false of the
     * given constraints after the satisfiability has been determined.
     *
     * @return an {@link ImmutableIntSet} containing the false variable IDs
     */
    ImmutableIntSet getModelFalsyVariables();

    /**
     * Resets the internal states of the solver instance, making it like one
     * newly created.
     */
    void reset();
}
