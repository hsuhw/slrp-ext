package core.automata.fsa;

import api.automata.Automaton;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;

import java.util.function.BiFunction;

public class DoubleMapDFSAManipulator implements FSAManipulator.Decorator
{
    private final FSAManipulator decoratee;

    public DoubleMapDFSAManipulator(FSAManipulator decoratee)
    {
        this.decoratee = decoratee;
    }

    @Override
    public FSAManipulator getDecoratee()
    {
        return decoratee;
    }

    private <S extends Symbol> boolean isImplementationTarget(Automaton<S> target)
    {
        return target instanceof DoubleMapDFSA<?>;
    }

    @Override
    public <S extends Symbol> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }

    @Override
    public <S extends Symbol, T extends Symbol, R extends Symbol> FSA<S> composeDelegated(Automaton<S> first,
                                                                                          Automaton<T> after,
                                                                                          BiFunction<S, T, R> composer)
    {
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> makeCompleteDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> minimizeDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }

    @Override
    public <S extends Symbol> FSA<S> getComplementDelegated(FSA<S> target)
    {
        if (!isImplementationTarget(target)) {
            return null;
        }
        return null;
    }
}
