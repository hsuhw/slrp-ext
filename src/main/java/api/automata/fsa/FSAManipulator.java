package api.automata.fsa;

import api.automata.AutomatonManipulator;
import api.automata.Symbol;

public interface FSAManipulator extends AutomatonManipulator
{
    <S extends Symbol> FSA<S> determinize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplete(FSA<S> target);

    <S extends Symbol> FSA<S> minimize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplement(FSA<S> target);

    <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two);

    <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two);
}
