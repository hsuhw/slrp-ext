package core.automata;

import api.automata.State;
import api.util.Values;

public class NamelessState implements State
{
    private static final String NAME_PREFIX = "$";
    private static final int GENERATED_NAME_LENGTH = 4;

    private final String name;

    public NamelessState()
    {
        name = NAME_PREFIX + Values.randomAlphanumeric(GENERATED_NAME_LENGTH, false);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
