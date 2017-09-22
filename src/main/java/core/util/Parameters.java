package core.util;

public final class Parameters
{
    public static final boolean IMPLICIT_PRECONDITION_RESPECTED = true;

    // TODO: [tuning] see if there's any effects of modifying these
    public static final int NONDETERMINISTIC_TRANSITION_CAPACITY = 5;
    public static final int SAT_SOLVER_MAX_VARIABLE_NUMBER = 1000000;
    public static final int SAT_SOLVER_MAX_CLAUSE_NUMBER = 1000000;
    public static final double ADDITIONAL_STATE_CAPACITY_MULTIPLIER = 1.85;

    public static int CURRENT_BIGGEST_ALPHABET_SIZE = 8;

    private Parameters()
    {
    }
}
