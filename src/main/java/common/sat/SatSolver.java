package common.sat;

import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * The API definition for the SAT solver functionalities we use in programs.
 */
public interface SatSolver
{
    boolean isVerbose();

    void setVerbose(boolean value);

    int getTimeoutInSec();

    void setTimeoutInSec(int s);

    long getTimeoutInMs();

    void setTimeoutInMs(long ms);

    int newFreeVariable();

    ImmutableIntList newFreeVariables(int howMany);

    default void setLiteralTruthy(int literal)
    {
        addClause(literal);
    }

    default void setLiteralsTruthy(int... literals)
    {
        for (var literal : literals) {
            setLiteralTruthy(literal);
        }
    }

    default void setLiteralFalsy(int literal)
    {
        addClause(-literal);
    }

    default void setLiteralsFalsy(int... literals)
    {
        for (var literal : literals) {
            setLiteralFalsy(literal);
        }
    }

    default void markAsEquivalent(int literal1, int literal2)
    {
        addClause(-literal1, literal2);
        addClause(literal1, -literal2);
    }

    default void markAllAsEquivalent(int... literals)
    {
        for (var i = 0; i < literals.length - 1; i++) {
            addClause(-literals[i], literals[i + 1]);
        }
        addClause(-literals[literals.length - 1], literals[0]);
    }

    /**
     * Behaves the same as {@link #markAllAsEquivalent(int...)}.
     */
    default void syncLiterals(int... literals)
    {
        markAllAsEquivalent(literals);
    }

    private void encodeGreaterEqualAt(int alreadyGreater, int greaterHere, int digit1, int digit2)
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

    /**
     * Makes the first given array of literals, if taken as a bit vector (zero
     * cell as the highest digit), have binary value greater equal than the
     * second given array.
     */
    default void markAsGreaterEqualInBinary(int[] bitArray1, int[] bitArray2)
    {
        if (bitArray1.length == 0 || bitArray2.length == 0) {
            throw new IllegalArgumentException("zero length array given");
        }
        final var lengthDelta = bitArray1.length - bitArray2.length;
        final var commonLength = Math.min(bitArray1.length, bitArray2.length);
        final var greaterAlreadyIndicators = newFreeVariables(commonLength + 1);
        if (lengthDelta > 0) { // array1 being longer

            // define greater in the longer part
            final var longerPartHasValue = new int[lengthDelta];
            System.arraycopy(bitArray1, 0, longerPartHasValue, 0, lengthDelta);
            final var longerPartGreater = greaterAlreadyIndicators.get(0);
            // longerPartHasValue <--> longerPartGreater
            addClauseIf(longerPartGreater, longerPartHasValue);
            for (var i = 0; i < lengthDelta; i++) {
                addImplication(bitArray1[i], longerPartGreater);
            }

            // define greater in the common part
            for (var i = 0; i < bitArray2.length; i++) {
                final var alreadyGreater = greaterAlreadyIndicators.get(i);
                final var greaterHere = greaterAlreadyIndicators.get(i + 1);
                encodeGreaterEqualAt(alreadyGreater, greaterHere, bitArray1[lengthDelta + i], bitArray2[i]);
            }
        } else {
            final var absLengthDelta = -lengthDelta;

            // define greater in the longer part
            setLiteralFalsy(greaterAlreadyIndicators.get(0));
            for (var i = 0; i < absLengthDelta; i++) {
                setLiteralFalsy(bitArray2[i]);
            }

            // define greater in the common part
            for (var i = 0; i < bitArray1.length; i++) {
                final var alreadyGreater = greaterAlreadyIndicators.get(i);
                final var greaterHere = greaterAlreadyIndicators.get(i + 1);
                encodeGreaterEqualAt(alreadyGreater, greaterHere, bitArray1[i], bitArray2[absLengthDelta + i]);
            }
        }
    }

    /**
     * Behaves the same as {@link #markAsGreaterEqualInBinary(int[], int[])}, but
     * accepting {@link ImmutableIntList} as the given bit arrays.
     */
    default void markAsGreaterEqualInBinary(ImmutableIntList bitArray1, ImmutableIntList bitArray2)
    {
        markAsGreaterEqualInBinary(bitArray1.toArray(), bitArray2.toArray());
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
     * Behaves the same as {@link #addClause(int...)}, but accepting an
     * {@link ImmutableIntSet} as the given clause.
     */
    default void addClause(ImmutableIntSet clause)
    {
        addClause(clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClause(int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the clause
     * @param clause    the clause to be added
     */
    default void addClauseIf(int indicator, int... clause)
    {
        final MutableIntList mutableClause = new IntArrayList(clause.length + 1);
        mutableClause.addAll(clause);
        mutableClause.add(-indicator);
        addClause(mutableClause.toArray());
    }

    /**
     * Behaves the same as {@link #addClauseIf(int, int...)}, but accepting an
     * {@link ImmutableIntList} as the given clause.
     */
    default void addClauseIf(int indicator, ImmutableIntList clause)
    {
        addClauseIf(indicator, clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClauseIf(int, int...)}, but accepting an
     * {@link ImmutableIntSet} as the given clause.
     */
    default void addClauseIf(int indicator, ImmutableIntSet clause)
    {
        addClauseIf(indicator, clause.toArray());
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
        final MutableIntList padding = new IntArrayList(degree + 1);
        padding.addAll(newFreeVariables(degree));
        final MutableIntList paddedClause = new IntArrayList(clause.length + padding.size());
        paddedClause.addAll(clause);
        paddedClause.addAll(padding);
        addClauseAtLeast(degree, paddedClause.toArray());

        // make the indicator the switch of the padding
        padding.add(-indicator);
        syncLiterals(padding.toArray());
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
        final var paddingLength = clause.length - degree;
        final MutableIntList padding = new IntArrayList(paddingLength + 1);
        padding.addAll(newFreeVariables(paddingLength));
        final MutableIntList paddedClause = new IntArrayList(clause.length + padding.size());
        paddedClause.addAll(clause);
        paddedClause.addAll(padding);
        addClauseAtMost(clause.length, paddedClause.toArray());

        padding.add(indicator);
        syncLiterals(padding.toArray());
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
        final var blockingClause = new int[clause.length];
        for (var i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        addClause(blockingClause);
    }

    /**
     * Behaves the same as {@link #addClauseBlocking(int...)}, but accepting an
     * {@link ImmutableIntSet} as the given clause.
     */
    default void addClauseBlocking(ImmutableIntSet clause)
    {
        addClauseBlocking(clause.toArray());
    }

    /**
     * Behaves the same as {@link #addClauseBlocking(int...)} if the given
     * {@code indicator} is valuated true.  If not activated, the given clause
     * has no any effects as if it were never mentioned.
     *
     * @param indicator whether to activate the blocking clause
     * @param clause    the clause to be added
     */
    default void addClauseBlockingIf(int indicator, int... clause)
    {
        final var blockingClause = new int[clause.length + 1];
        for (var i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        blockingClause[clause.length] = -indicator;
        addClause(blockingClause);
    }

    /**
     * Behaves the same as {@link #addClauseBlockingIf(int, int...)}, but
     * accepting an {@link ImmutableIntSet} as the given clause.
     */
    default void addClauseBlockingIf(int indicator, ImmutableIntSet clause)
    {
        addClauseBlockingIf(indicator, clause.toArray());
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
        for (var consequent : consequents) {
            addImplication(antecedent, consequent);
        }
    }

    /**
     * Behaves the same as {@link #addImplicationIf(int, int, int)}, but
     * accepting multiple consequents.
     */
    default void addImplicationsIf(int indicator, int antecedent, int... consequents)
    {
        for (var consequent : consequents) {
            addImplicationIf(indicator, antecedent, consequent);
        }
    }

    /**
     * Determines whether the given constraints can be satisfied.
     */
    boolean findItSatisfiable();

    /**
     * Returns the model of the given constraints after the satisfiability has
     * been determined.
     *
     * @return an {@link IntSet} containing the true variable IDs in
     * positive {@code int} and the false variable IDs in negative {@code int}
     */
    ImmutableIntSet getModel();

    /**
     * Returns the variables in the model that have been assigned true of the
     * given constraints after the satisfiability has been determined.
     *
     * @return an {@link IntSet} containing the true variable IDs
     */
    ImmutableIntSet getModelTruthyVariables();

    /**
     * Returns the variables in the model that have been assigned false of the
     * given constraints after the satisfiability has been determined.
     *
     * @return an {@link IntSet} containing the false variable IDs
     */
    IntSet getModelFalsyVariables();

    /**
     * Resets the internal states of the solver instance, making it like one
     * newly created.
     */
    void reset();
}
