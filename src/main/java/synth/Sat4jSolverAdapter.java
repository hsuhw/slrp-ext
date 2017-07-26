package synth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.block.factory.primitive.IntPredicates;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;
import java.util.Optional;

public class Sat4jSolverAdapter implements SatSolver
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final ISolver solver;
    private int nextFreeVariableId = 1;
    private ImmutableIntSet model;

    public Sat4jSolverAdapter()
    {
        solver = SolverFactory.newDefault(); // TODO: [tuning] see if there's any effects of modifying this
        solver.newVar(MAX_VARIABLE_NUMBER);
        solver.setExpectedNumberOfClauses(MAX_CLAUSE_NUMBER);
    }

    private void assertModelValid() throws UnsupportedOperationException
    {
        if (model == null) {
            throw new UnsupportedOperationException("try to get the valuation before a model found");
        }
    }

    @Override
    public boolean isVerbose()
    {
        return solver.isVerbose();
    }

    @Override
    public void setVerbose(boolean v)
    {
        solver.setVerbose(v);
    }

    @Override
    public int getTimeoutInSec()
    {
        return solver.getTimeout();
    }

    @Override
    public void setTimeoutInSec(int t)
    {
        solver.setTimeout(t);
    }

    @Override
    public long getTimeoutInMs()
    {
        return solver.getTimeoutMs();
    }

    @Override
    public void setTimeoutInMs(long t)
    {
        solver.setTimeoutMs(t);
    }

    @Override
    public IntInterval newFreeVariables(int howMany)
    {
        if (howMany < 1) {
            final String msg = "asking for " + howMany + " new variables";
            throw new IllegalArgumentException(msg);
        }
        if (nextFreeVariableId + howMany > MAX_VARIABLE_NUMBER) {
            final String msg = "ran out of available free variables";
            throw new UnsupportedOperationException(msg);
        }
        final int from = nextFreeVariableId;
        nextFreeVariableId += howMany;
        final int to = nextFreeVariableId - 1;
        return IntInterval.fromTo(from, to);
    }

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
    public void addClause(int... clause) throws UnsupportedOperationException
    {
        model = null;
        try {
            solver.addClause(new VecInt(clause));
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding clause ";
            throw new UnsupportedOperationException(msg + Arrays.toString(clause));
        }
    }

    @Override
    public void addClause(IntInterval clause)
    {
        addClause(clause.toArray());
    }

    @Override
    public void addClauseAtLeast(int degree, int... clause)
    {
        model = null;
        try {
            solver.addAtLeast(new VecInt(clause), degree);
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding at-least-" + degree + " clause ";
            throw new UnsupportedOperationException(msg + Arrays.toString(clause));
        }
    }

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
    public void addClauseAtMost(int degree, int... clause)
    {
        model = null;
        try {
            solver.addAtMost(new VecInt(clause), degree);
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding at-most-" + degree + " clause ";
            throw new UnsupportedOperationException(msg + Arrays.toString(clause));
        }
    }

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
    public void addClauseExactly(int degree, int... clause)
    {
        model = null;
        try {
            solver.addExactly(new VecInt(clause), degree);
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding exact clause ";
            throw new UnsupportedOperationException(msg + Arrays.toString(clause));
        }
    }

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
        // since a current bug of SAT4J, we implement the clause blocking our way
        final int[] blockingClause = new int[clause.length];
        for (int i = 0; i < clause.length; i++) {
            blockingClause[i] = -clause[i];
        }
        addClause(blockingClause);
    }

    @Override
    public void addClauseBlockingIf(int indicator, int... clause)
    {
        // since a current bug of SAT4J, we implement the clause blocking our way
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
    public Optional<Boolean> findItSatisfiable()
    {
        if (model != null) {
            return Optional.of(Boolean.TRUE);
        }
        LOGGER.debug("Invoke a solving on SAT4J.");
        final long startTime = System.currentTimeMillis();
        try {
            if (solver.isSatisfiable()) {
                final long endTime = System.currentTimeMillis();
                LOGGER.info("SAT4J found a solution in {}ms.", endTime - startTime);
                model = IntSets.immutable.of(solver.model());
                return Optional.of(Boolean.TRUE);
            }
        } catch (TimeoutException e) {
            final long endTime = System.currentTimeMillis();
            LOGGER.warn("SAT4J failed to solve the problem within {}ms.", endTime - startTime);
            return Optional.empty();
        }
        return Optional.of(Boolean.FALSE);
    }

    @Override
    public Optional<ImmutableIntSet> findModel()
    {
        return findItSatisfiable().map(found -> found ? getModel() : null);
    }

    @Override
    public ImmutableIntSet getModel()
    {
        assertModelValid();
        return model;
    }

    @Override
    public ImmutableIntSet getModelTruthyVariables() throws UnsupportedOperationException
    {
        assertModelValid();
        return model.select(IntPredicates.greaterThan(0));
    }

    @Override
    public ImmutableIntSet getModelFalsyVariables() throws UnsupportedOperationException
    {
        assertModelValid();
        return model.select(IntPredicates.lessThan(0));
    }

    @Override
    public void reset()
    {
        solver.reset();
        solver.newVar(MAX_VARIABLE_NUMBER);
        nextFreeVariableId = 1;
        model = null;
    }
}
