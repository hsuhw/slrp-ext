package core.automata.fsa;

import api.automata.Alphabet;
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
    public <S extends Symbol> Automaton<S> trimDeadEndStates(Automaton<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol, T extends Symbol, R extends Symbol> Automaton<R> makeProduct(Automaton<S> first,
                                                                                           Automaton<T> after,
                                                                                           Alphabet<R> targetAlphabet,
                                                                                           BiFunction<S, T, R> transitionDecider,
                                                                                           StateAttributeDecider<R> stateAttributeDecider)
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
    public <S extends Symbol> FSA<S> makeComplement(FSA<S> target)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }

    @Override
    public <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        throw new UnsupportedOperationException(Misc.NMI);
    }
}
