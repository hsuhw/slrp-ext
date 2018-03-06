package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableState;
import api.automata.fsa.MutableFSA;
import core.automata.AbstractMutableAutomaton;

public abstract class AbstractMutableFSA<S> extends AbstractMutableAutomaton<S> implements MutableFSA<S>
{
    public AbstractMutableFSA(Alphabet<S> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);
    }

    public AbstractMutableFSA(AbstractMutableFSA<S> toCopy, boolean deep)
    {
        super(toCopy, deep);
    }

    @Override
    public MutableFSA<S> addState(MutableState<S> state)
    {
        return (MutableFSA<S>) super.addState(state);
    }

    @Override
    public MutableFSA<S> removeState(MutableState<S> state)
    {
        return (MutableFSA<S>) super.removeState(state);
    }

    @Override
    public MutableFSA<S> setAsStart(MutableState<S> state)
    {
        return (MutableFSA<S>) super.setAsStart(state);
    }

    @Override
    public MutableFSA<S> setAsAccept(MutableState<S> state)
    {
        return (MutableFSA<S>) super.setAsAccept(state);
    }

    @Override
    public MutableFSA<S> unsetAccept(MutableState<S> state)
    {
        return (MutableFSA<S>) super.unsetAccept(state);
    }

    @Override
    public MutableFSA<S> resetAcceptStates()
    {
        return (MutableFSA<S>) super.resetAcceptStates();
    }

    @Override
    public MutableFSA<S> addTransition(MutableState<S> dept, MutableState<S> dest, S symbol)
    {
        return (MutableFSA<S>) super.addTransition(dept, dest, symbol);
    }
}
