package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableState;
import api.automata.fsa.MutableFSA;
import core.automata.AbstractMutableAutomaton;

public abstract class AbstractMutableFSA<S extends MutableState<T>, T> extends AbstractMutableAutomaton<S, T>
    implements MutableFSA<S, T>
{
    public AbstractMutableFSA(Alphabet<T> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);
    }

    public AbstractMutableFSA(AbstractMutableFSA<S, T> toBeCopied, boolean deep)
    {
        super(toBeCopied, deep);
    }

    @Override
    public MutableFSA<S, T> addState(S state)
    {
        return (MutableFSA<S, T>) super.addState(state);
    }

    @Override
    public MutableFSA<S, T> removeState(S state)
    {
        return (MutableFSA<S, T>) super.removeState(state);
    }

    @Override
    public MutableFSA<S, T> setAsStart(S state)
    {
        return (MutableFSA<S, T>) super.setAsStart(state);
    }

    @Override
    public MutableFSA<S, T> setAsAccept(S state)
    {
        return (MutableFSA<S, T>) super.setAsAccept(state);
    }

    @Override
    public MutableFSA<S, T> unsetAccept(S state)
    {
        return (MutableFSA<S, T>) super.unsetAccept(state);
    }

    @Override
    public MutableFSA<S, T> resetAcceptStates()
    {
        return (MutableFSA<S, T>) super.resetAcceptStates();
    }

    @Override
    public MutableFSA<S, T> addTransition(S dept, S dest, T symbol)
    {
        return (MutableFSA<S, T>) super.addTransition(dept, dest, symbol);
    }
}
