package synth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.block.factory.primitive.IntPredicates;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;

public class Sat4jSolver implements SatSolver
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final ISolver solver;
    private ImmutableIntSet model;
    private int nextFreeVariableId = 1;

    public Sat4jSolver()
    {
        solver = SolverFactory.newGlucose(); // TODO: [tuning] see if there's any effects of modifying this
        solver.newVar(MAX_VARIABLE_NUM);
        solver.setExpectedNumberOfClauses(MAX_CLAUSE_NUM);
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
    public int getUsedVariableNumber()
    {
        return nextFreeVariableId - 1;
    }

    @Override
    public IntInterval newFreeVariables(int howMany)
    {
        final int from = nextFreeVariableId;
        nextFreeVariableId += howMany;
        final int to = nextFreeVariableId - 1;
        return IntInterval.fromTo(from, to);
    }

    @Override
    public int getClauseNumber()
    {
        return solver.nConstraints();
    }

    public void setLiteralTruthy(int literal)
    {
        model = null;
        try {
            solver.addClause(new VecInt(new int[]{literal}));
        } catch (ContradictionException e) {
            final String msg = "contradiction found when setting literal " + literal + " true";
            throw new UnsupportedOperationException(msg);
        }
    }

    public void setLiteralFalsy(int literal)
    {
        model = null;
        try {
            solver.addClause(new VecInt(new int[]{-literal}));
        } catch (ContradictionException e) {
            final String msg = "contradiction found when setting literal " + literal + " false";
            throw new UnsupportedOperationException(msg);
        }
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
    public void addClauseBlocking(int... clause)
    {
        model = null;
        try {
            solver.addBlockingClause(new VecInt(clause));
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding blocking clause ";
            throw new UnsupportedOperationException(msg + Arrays.toString(clause));
        }
    }

    @Override
    public void addClauseBlocking(IntInterval clause)
    {
        addClauseBlocking(clause.toArray());
    }

    @Override
    public void addImplication(int antecedent, int consequent)
    {
        addClause(-antecedent, consequent);
    }

    @Override
    public void addImplication(int antecedent, int... consequents)
    {
        for (int consequent : consequents) {
            addImplication(antecedent, consequent);
        }
    }

    @Override
    public boolean solveSuccessfully()
    {
        LOGGER.info("Invoke a solving on SAT4J.");
        final long startTime = System.currentTimeMillis();
        try {
            if (solver.isSatisfiable()) {
                final long endTime = System.currentTimeMillis();
                LOGGER.info("SAT4J found a solution in {}ms.", endTime - startTime);
                model = IntSets.immutable.of(solver.model());
                return true;
            }
        } catch (TimeoutException e) {
            final long endTime = System.currentTimeMillis();
            LOGGER.warn("SAT4J failed to solve the problem within {}ms.", endTime - startTime);
        }
        return false;
    }

    @Override
    public ImmutableIntSet getTruthyVariables() throws UnsupportedOperationException
    {
        assertModelValid();
        return model.select(IntPredicates.greaterThan(0));
    }

    @Override
    public ImmutableIntSet getFalsyVariables() throws UnsupportedOperationException
    {
        assertModelValid();
        return model.select(IntPredicates.lessThan(0));
    }

    @Override
    public void reset()
    {
        model = null;
        solver.reset();
        solver.newVar(MAX_VARIABLE_NUM);
    }
}
