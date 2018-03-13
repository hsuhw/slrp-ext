package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableState;
import api.automata.fsa.FSA;
import api.automata.fsa.MutableFSA;
import core.automata.AbstractMutableAutomaton;

public abstract class AbstractMutableFSA<S> extends AbstractMutableAutomaton<S> implements MutableFSA<S>
{
    private FSA<S> determinized;
    private FSA<S> completed;
    private FSA<S> minimized;
    private FSA<S> complemented;

    public AbstractMutableFSA(Alphabet<S> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);
    }

    public AbstractMutableFSA(AbstractMutableAutomaton<S> toCopy, boolean deep)
    {
        super(toCopy, deep);
    }

    @Override
    public FSA<S> determinize()
    {
        if (!hasChanged && determinized != null) {
            return determinized;
        }

        return (determinized = MutableFSA.super.determinize());
    }

    @Override
    public FSA<S> complete()
    {
        if (!hasChanged && completed != null) {
            return completed;
        }

        return (completed = MutableFSA.super.complete());
    }

    @Override
    public FSA<S> minimize()
    {
        if (!hasChanged && minimized != null) {
            return minimized;
        }

        return (minimized = MutableFSA.super.minimize());
    }

    @Override
    public FSA<S> complement()
    {
        if (!hasChanged && complemented != null) {
            return complemented;
        }

        return (complemented = MutableFSA.super.complement());
    }

    @Override
    public MutableFSA<S> setAlphabet(Alphabet<S> alphabet)
    {
        return (MutableFSA<S>) super.setAlphabet(alphabet);
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
