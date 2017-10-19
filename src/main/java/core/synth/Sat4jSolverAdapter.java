package core.synth;

import api.synth.SatSolver;
import api.synth.SatSolverTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.util.Arrays;

import static core.util.Parameters.SAT_SOLVER_MAX_CLAUSE_NUMBER;
import static core.util.Parameters.SAT_SOLVER_MAX_VARIABLE_NUMBER;

public class Sat4jSolverAdapter implements SatSolver
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ImmutableIntSet NONSOLUTION = IntSets.immutable.empty();

    private final ISolver solver;
    private int nextFreeVariableId = 1;
    private ImmutableIntSet model;

    public Sat4jSolverAdapter()
    {
        solver = SolverFactory.newDefault(); // TODO: [tuning] see if there's any effects of modifying this
        solver.newVar(SAT_SOLVER_MAX_VARIABLE_NUMBER);
        solver.setExpectedNumberOfClauses(SAT_SOLVER_MAX_CLAUSE_NUMBER);
    }

    private void assertModelValid()
    {
        if (model == null || model == NONSOLUTION) {
            throw new IllegalStateException("try to get the valuation before a model found");
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
    public ImmutableIntList newFreeVariables(int howMany)
    {
        if (howMany < 1) {
            throw new IllegalArgumentException("number given cannot be less than 1");
        }
        if (nextFreeVariableId + howMany > SAT_SOLVER_MAX_VARIABLE_NUMBER) {
            throw new IllegalArgumentException("ran out of available free variables");
        }

        final int from = nextFreeVariableId;
        nextFreeVariableId += howMany;
        final int to = nextFreeVariableId - 1;
        return IntInterval.fromTo(from, to);
    }

    @Override
    public void addClause(int... clause)
    {
        model = null;
        try {
            solver.addClause(new VecInt(clause));
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding clause ";
            throw new IllegalArgumentException(msg + Arrays.toString(clause));
        }
    }

    @Override
    public void addClauseAtLeast(int degree, int... clause)
    {
        model = null;
        try {
            solver.addAtLeast(new VecInt(clause), degree);
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding at-least-" + degree + " clause ";
            throw new IllegalArgumentException(msg + Arrays.toString(clause));
        }
    }

    @Override
    public void addClauseAtMost(int degree, int... clause)
    {
        model = null;
        try {
            solver.addAtMost(new VecInt(clause), degree);
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding at-most-" + degree + " clause ";
            throw new IllegalArgumentException(msg + Arrays.toString(clause));
        }
    }

    @Override
    public void addClauseExactly(int degree, int... clause)
    {
        model = null;
        try {
            solver.addExactly(new VecInt(clause), degree);
        } catch (ContradictionException e) {
            final String msg = "contradiction found when adding exact clause ";
            throw new IllegalArgumentException(msg + Arrays.toString(clause));
        }
    }

    @Override
    public boolean findItSatisfiable() throws SatSolverTimeoutException
    {
        if (model != null) {
            return model != NONSOLUTION;
        }
        LOGGER.debug("Invoke a solving on SAT4J.");
        final long startTime = System.currentTimeMillis();
        long endTime;
        try {
            if (solver.isSatisfiable()) {
                endTime = System.currentTimeMillis();
                LOGGER.info("SAT4J found a solution in {}ms.", endTime - startTime);
                model = IntSets.immutable.of(solver.model());
                return true;
            }
        } catch (TimeoutException e) {
            endTime = System.currentTimeMillis();
            LOGGER.warn("SAT4J failed to solve the problem within {}ms.", endTime - startTime);
            throw new SatSolverTimeoutException();
        }
        endTime = System.currentTimeMillis();
        LOGGER.info("SAT4J found it unsatisfiable in {}ms.", endTime - startTime);
        model = NONSOLUTION;
        return false;
    }

    @Override
    public ImmutableIntSet getModel()
    {
        assertModelValid();
        return model;
    }

    @Override
    public ImmutableIntSet getModelTruthyVariables()
    {
        assertModelValid();
        return model.select(x -> x > 0);
    }

    @Override
    public IntSet getModelFalsyVariables()
    {
        assertModelValid();
        return model.select(x -> x < 0).collectInt(x -> -x, new IntHashSet(model.size())); // upper bound, one-off
    }

    @Override
    public void reset()
    {
        solver.reset();
        solver.newVar(SAT_SOLVER_MAX_VARIABLE_NUMBER);
        nextFreeVariableId = 1;
        model = null;
    }
}
