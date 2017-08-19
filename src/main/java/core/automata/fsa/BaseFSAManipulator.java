package core.automata.fsa;

import api.automata.Automaton;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import util.Misc;

import java.util.function.BiFunction;

public class BaseFSAManipulator implements FSAManipulator
{
    @Override
    public <S extends Symbol> FSA<S> trimUnreachableStates(Automaton<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol, T extends Symbol, R extends Symbol> FSA<S> compose(Automaton<S> first, Automaton<T> after,
                                                                                 BiFunction<S, T, R> composer)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol> FSA<S> determinize(FSA<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol> FSA<S> makeComplete(FSA<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol> FSA<S> minimize(FSA<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol> FSA<S> getComplement(FSA<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }
}
