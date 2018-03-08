package core.automata.fst;

import api.automata.Alphabet;
import api.automata.MutableState;
import api.automata.fst.FST;
import api.automata.fst.MutableFST;
import core.automata.AbstractMutableAutomaton;
import org.eclipse.collections.api.tuple.Pair;

public abstract class AbstractMutableFST<S, T> extends AbstractMutableAutomaton<Pair<S, T>> implements MutableFST<S, T>
{
    private Alphabet<S> inputAlphabet;
    private Alphabet<T> outputAlphabet;
    private FST<T, S> inverse;

    public AbstractMutableFST(Alphabet<Pair<S, T>> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);
    }

    public AbstractMutableFST(AbstractMutableFST<S, T> toCopy, boolean deep)
    {
        super(toCopy, deep);
    }

    @Override
    public MutableFST<S, T> setAlphabet(Alphabet<Pair<S, T>> alphabet)
    {
        return (MutableFST<S, T>) super.setAlphabet(alphabet);
    }

    @Override
    public MutableFST<S, T> addState(MutableState<Pair<S, T>> state)
    {
        return (MutableFST<S, T>) super.addState(state);
    }

    @Override
    public MutableFST<S, T> removeState(MutableState<Pair<S, T>> state)
    {
        return (MutableFST<S, T>) super.removeState(state);
    }

    @Override
    public MutableFST<S, T> setAsStart(MutableState<Pair<S, T>> state)
    {
        return (MutableFST<S, T>) super.setAsStart(state);
    }

    @Override
    public MutableFST<S, T> setAsAccept(MutableState<Pair<S, T>> state)
    {
        return (MutableFST<S, T>) super.setAsAccept(state);
    }

    @Override
    public MutableFST<S, T> unsetAccept(MutableState<Pair<S, T>> state)
    {
        return (MutableFST<S, T>) super.unsetAccept(state);
    }

    @Override
    public MutableFST<S, T> resetAcceptStates()
    {
        return (MutableFST<S, T>) super.resetAcceptStates();
    }

    @Override
    public MutableFST<S, T> addTransition(MutableState<Pair<S, T>> dept, MutableState<Pair<S, T>> dest,
        Pair<S, T> symbol)
    {
        return (MutableFST<S, T>) super.addTransition(dept, dest, symbol);
    }

    @Override
    public Alphabet<S> inputAlphabet()
    {
        if (!hasChanged && inputAlphabet != null) {
            return inputAlphabet;
        }

        return (inputAlphabet = MutableFST.super.inputAlphabet());
    }

    @Override
    public Alphabet<T> outputAlphabet()
    {
        if (!hasChanged && outputAlphabet != null) {
            return outputAlphabet;
        }

        return (outputAlphabet = MutableFST.super.outputAlphabet());
    }

    @Override
    public FST<T, S> inverse()
    {
        if (!hasChanged && inverse != null) {
            return inverse;
        }

        return (inverse = MutableFST.super.inverse());
    }
}
