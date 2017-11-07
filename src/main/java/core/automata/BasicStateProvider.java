package core.automata;

import api.automata.State;

public final class BasicStateProvider implements State.Provider
{
    @Override
    public State create(String name)
    {
        return new NamedState(name);
    }

    @Override
    public State generate()
    {
        return new NamelessState();
    }
}
