package api.automata.fsa;

import api.automata.*;
import core.automata.fsa.BasicFSAStateAttributes;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.impl.block.factory.primitive.BooleanPredicates;

import java.util.function.BiFunction;

public interface FSAManipulator extends AutomatonManipulator
{
    @Override
    <S extends Symbol> FSA<S> trimUnreachableStates(Automaton<S> target);

    @Override
    <S extends Symbol> FSA<S> trimDeadEndStates(Automaton<S> target);

    @Override
    <S extends Symbol, T extends Symbol, R extends Symbol> FSA<R> makeProduct(Automaton<S> one, Automaton<T> two,
                                                                              Alphabet<R> targetAlphabet,
                                                                              BiFunction<S, T, R> transitionDecider,
                                                                              StateAttributeDecider<R> stateAttributeDecider);

    <S extends Symbol> FSA<S> determinize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplete(FSA<S> target);

    <S extends Symbol> FSA<S> minimize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplement(FSA<S> target);

    private <S extends Symbol> S matchedSymbol(S one, S two)
    {
        return one.equals(two) ? one : null;
    }

    default <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        return makeProduct(one, two, one.getAlphabet(), this::matchedSymbol, (stateMapping, delta) -> {
            final ImmutableList<State> states = stateMapping.keysView().toList().toImmutable();
            final ImmutableBooleanList startStateTable = makeStartStateIntersection(states, stateMapping, one, two);
            final ImmutableBooleanList acceptStateTable = makeAcceptStateIntersection(states, stateMapping, one, two);
            return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
        });
    }

    default <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        return makeProduct(one, two, one.getAlphabet(), this::matchedSymbol, (stateMapping, delta) -> {
            final ImmutableList<State> states = stateMapping.keysView().toList().toImmutable();
            final ImmutableBooleanList startStateTable = makeStartStateIntersection(states, stateMapping, one, two);
            final ImmutableBooleanList acceptStateTable = makeAcceptStateUnion(states, stateMapping, one, two);
            return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
        });
    }

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
