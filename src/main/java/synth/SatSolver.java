package synth;

import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.primitive.IntInterval;

public interface SatSolver
{
    int MAX_VARIABLE_NUM = 1000000;
    int MAX_CLAUSE_NUM = 1000000; // TODO: [tuning] see if there's any effects of modifying this

    boolean isVerbose();

    void setVerbose(boolean value);

    int getTimeoutInSec();

    void setTimeoutInSec(int s);

    long getTimeoutInMs();

    void setTimeoutInMs(long ms);

    int getUsedVariableNumber();

    IntInterval newFreeVariables(int howMany);

    int getClauseNumber();

    void addClause(int[] clause);

    boolean solveSuccessfully();

    ImmutableIntSet getTruthyVariables();

    ImmutableIntSet getFalsyVariables();

    void reset();
}
