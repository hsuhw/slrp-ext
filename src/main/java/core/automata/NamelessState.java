package core.automata;

import api.automata.State;
import common.util.Random;

public class NamelessState implements State
{
    public static final String NAME_PREFIX = "$";

    private static final int GENERATED_NAME_LENGTH = 4;

    private final String name;

    public NamelessState()
    {
        name = NAME_PREFIX + Random.alphanumeric(GENERATED_NAME_LENGTH, false);
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name();
    }
}
