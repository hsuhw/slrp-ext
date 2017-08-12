package core.synth;

import api.synth.SatSolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
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

public class Sat4jSolverAdapter extends AbstractSatSolver implements SatSolver
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

    private void assertModelValid()
    {
        if (model == null) {
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
            final String msg = "asking for " + howMany + " new variables";
            throw new IllegalArgumentException(msg);
        }
        if (nextFreeVariableId + howMany > MAX_VARIABLE_NUMBER) {
            final String msg = "ran out of available free variables";
            throw new IllegalArgumentException(msg);
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
    public Boolean findItSatisfiable()
    {
        if (model != null) {
            return Boolean.TRUE;
        }
        LOGGER.debug("Invoke a solving on SAT4J.");
        final long startTime = System.currentTimeMillis();
        try {
            if (solver.isSatisfiable()) {
                final long endTime = System.currentTimeMillis();
                LOGGER.info("SAT4J found a solution in {}ms.", endTime - startTime);
                model = IntSets.immutable.of(solver.model());
                return Boolean.TRUE;
            }
        } catch (TimeoutException e) {
            final long endTime = System.currentTimeMillis();
            LOGGER.warn("SAT4J failed to solve the problem within {}ms.", endTime - startTime);
            return null;
        }
        return Boolean.FALSE;
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
        return model.select(IntPredicates.greaterThan(0));
    }

    @Override
    public ImmutableIntSet getModelFalsyVariables()
    {
        assertModelValid();
        ImmutableIntSet falsyVariablesAsSet = model.select(IntPredicates.lessThan(0));
        return falsyVariablesAsSet.collectInt(x -> -x, IntSets.mutable.empty()).toSet().toImmutable();
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
