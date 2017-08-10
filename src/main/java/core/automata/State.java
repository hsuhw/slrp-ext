package core.automata;

public class State implements api.automata.State
{
    private final String name;

    public State(String name)
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
