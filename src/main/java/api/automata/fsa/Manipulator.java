package api.automata.fsa;

import api.automata.Symbol;

public interface Manipulator extends api.automata.Manipulator
{
    <S extends Symbol> FSA<S> determinize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplete(FSA<S> target);

    <S extends Symbol> FSA<S> minize(FSA<S> target);

    <S extends Symbol> FSA<S> getComplement(FSA<S> target);
}
