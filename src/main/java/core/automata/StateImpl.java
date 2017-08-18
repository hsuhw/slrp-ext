package core.automata;

import api.automata.State;

public class StateImpl implements State
{
    private final String name;

    public StateImpl(String name)
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
