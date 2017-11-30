package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.LanguageSubsetChecker;

import java.util.function.Function;

import static api.util.Values.NO_MATCHED_IMPLEMENTATION_FOUND;

public final class BaseFSAManipulator implements FSAManipulator
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
    public <S> FSA<S> trimDanglingStates(Automaton<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S, R> FSA<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                    SymbolDecider<S, T, R> transitionDecider, Finalizer<R> finalizer)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                    StepFilter<S, T, R> stepFilter, Finalizer<R> finalizer)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> determinize(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> complete(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> minimize(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> FSA<S> complement(FSA<S> target)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }

    @Override
    public <S> LanguageSubsetChecker.Result<S> checkSubset(FSA<S> subsumer, FSA<S> includer)
    {
        throw new UnsupportedOperationException(NO_MATCHED_IMPLEMENTATION_FOUND);
    }
}
