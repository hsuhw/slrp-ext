package core;

public final class Parameters
{
    // TODO: [tuning] see if there's any effects of modifying these
    public static final int NONDETERMINISTIC_TRANSITION_CAPACITY = 7;
    public static final int SAT_SOLVER_MAX_VARIABLE_NUMBER = 1000000;
    public static final int SAT_SOLVER_MAX_CLAUSE_NUMBER = 1000000;
    public static final int PARSER_COMMON_CAPACITY = 10;
    public static final double ADDITIONAL_CAPACITY_MULTIPLIER = 1.85;

    private Parameters()
    {
    }

    public static int estimateExtendedSize(int originalSize)
    {
        return (int) Math.round(originalSize * ADDITIONAL_CAPACITY_MULTIPLIER);
    }
}
