package core.automata;

import api.automata.State;
import util.Misc;

public class GeneratedState implements State
{
    private static final String NAME_PREFIX = "$";
    private static final int GENERATED_NAME_LENGTH = 4;

    private final String name;

    public GeneratedState()
    {
        this.name = NAME_PREFIX + Misc.randomAlphanumeric(GENERATED_NAME_LENGTH, false);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
