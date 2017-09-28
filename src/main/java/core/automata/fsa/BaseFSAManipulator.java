package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;

import java.util.function.BiFunction;

import static api.util.Values.NO_MATCHED_IMPLEMENTATION_FOUND;

public class BaseFSAManipulator implements FSAManipulator
{
    @Override
    public <S> FSA<S> trimUnreachableStates(Automaton<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> trimDeadEndStates(Automaton<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S, T, R> FSA<R> makeProduct(Automaton<S> first, Automaton<T> after, Alphabet<R> targetAlphabet,
                                        BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> determinize(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> makeComplete(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> minimize(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> makeComplement(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }
}
