package core.automata;

import api.automata.State;

public class ParsedState implements State
{
    private final String name;

    public ParsedState(String name)
    {
        this.name = name;
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
