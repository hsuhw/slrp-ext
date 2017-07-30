package synth;

import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.primitive.IntInterval;

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

    IntInterval newFreeVariables(int howMany);

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
     * Adds a DIMACS-CNF format clause to the problem.
     *
     * @param clause the clause to be added
     */
    void addClause(int... clause);

    /**
     * Behaves the same as {@link #addClause(int...)}, but accepting an
     * {@link IntInterval} as the given clause.
     */
    void addClause(IntInterval clause);

    /**
     * Behaves the same as {@link #addClause(IntInterval)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the clause
     * @param clause    the clause to be added
     */
    void addClauseIf(int indicator, IntInterval clause);

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
     * accepting an {@link IntInterval} as the given clause.
     */
    void addClauseAtLeast(int degree, IntInterval clause);

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
     * accepting an {@link IntInterval} as the given clause.
     */
    void addClauseAtLeastIf(int indicator, int degree, IntInterval clause);

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
     * an {@link IntInterval} as the given clause.
     */
    void addClauseAtMost(int degree, IntInterval clause);

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
     * accepting an {@link IntInterval} as the given clause.
     */
    void addClauseAtMostIf(int indicator, int degree, IntInterval clause);

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
     * accepting an {@link IntInterval} as the given clause.
     */
    void addClauseExactly(int degree, IntInterval clause);

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
     * accepting an {@link IntInterval} as the given clause.
     */
    void addClauseExactlyIf(int indicator, int degree, IntInterval clause);

    /**
     * Prevents the given clause (with all its literals assigned true) showing
     * up as an model (or as an subset of the model).
     *
     * @param clause the clause to be blocked
     */
    void addClauseBlocking(int... clause);

    /**
     * Behaves the same as {@link #addClauseBlocking(int...)}, but accepting an
     * {@link IntInterval} as the given clause.
     */
    void addClauseBlockingIf(int indicator, int... clause);

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

    Optional<Boolean> findItSatisfiable();

    Optional<ImmutableIntSet> findModel();

    ImmutableIntSet getModel();

    ImmutableIntSet getModelTruthyVariables();

    ImmutableIntSet getModelFalsyVariables();

    void reset();
}
