package core.automata;

import api.automata.State;

public class NamedState implements State
{
    private final String name;

    public NamedState(String name)
    {
        this.name = name;
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
