package api.automata.fsa;

import api.automata.AutomatonManipulator;
import api.automata.Symbol;
import org.eclipse.collections.impl.block.factory.primitive.BooleanPredicates;

public interface FSAManipulator extends AutomatonManipulator
{
    <S extends Symbol> FSA<S> determinize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplete(FSA<S> target);

    <S extends Symbol> FSA<S> minimize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplement(FSA<S> target);

    <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two);

    <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two);

    default <S extends Symbol> boolean checkLanguageEmpty(FSA<S> target)
    {
        return !trimUnreachableStates(target).getAcceptStateTable().anySatisfy(BooleanPredicates.isTrue());
    }

    default <S extends Symbol> boolean checkLanguageSigmaStar(FSA<S> target)
    {
        return checkLanguageEmpty(makeComplement(target));
    }

    default <S extends Symbol> boolean checkLanguageContainment(FSA<S> container, FSA<S> subset)
    {
        return checkLanguageEmpty(makeIntersection(container, makeComplement(subset)));
    }
}
