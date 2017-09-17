package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;
import api.automata.Symbol;
import core.automata.fsa.BasicFSAStateAttributes;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

import java.util.function.BiFunction;

public interface FSAManipulatorDecorator extends FSAManipulator
{
    FSAManipulator getDecoratee();

    <S extends Symbol> boolean isImplementationTarget(Automaton<S> target);

    <S extends Symbol> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target);

    @Override
    default <S extends Symbol> FSA<S> trimUnreachableStates(Automaton<S> target)
    {
        if (isImplementationTarget(target)) {
            return trimUnreachableStatesDelegated(target);
        }
        return (FSA<S>) getDecoratee().trimUnreachableStates(target);
    }

    <S extends Symbol> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target);

    @Override
    default <S extends Symbol> FSA<S> trimDeadEndStates(Automaton<S> target)
    {
        if (isImplementationTarget(target)) {
            return trimDeadEndStatesDelegated(target);
        }
        return (FSA<S>) getDecoratee().trimDeadEndStates(target);
    }

    <S extends Symbol, T extends Symbol, R extends Symbol> FSA<R> makeProductDelegated(Automaton<S> one,
                                                                                       Automaton<T> two,
                                                                                       Alphabet<R> targetAlphabet,
                                                                                       BiFunction<S, T, R> transitionDecider,
                                                                                       StateAttributeDecider<R> stateAttributeDecider);

    @Override
    default <S extends Symbol, T extends Symbol, R extends Symbol> FSA<R> makeProduct(Automaton<S> one,
                                                                                      Automaton<T> two,
                                                                                      Alphabet<R> targetAlphabet,
                                                                                      BiFunction<S, T, R> transitionDecider,
                                                                                      StateAttributeDecider<R> stateAttributeDecider)
    {
        if (isImplementationTarget(one) && isImplementationTarget(two)) {
            return makeProductDelegated(one, two, targetAlphabet, transitionDecider, stateAttributeDecider);
        }
        return (FSA<R>) getDecoratee().makeProduct(one, two, targetAlphabet, transitionDecider, stateAttributeDecider);
    }

    <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> determinize(FSA<S> target)
    {
        if (isImplementationTarget(target)) {
            return determinizeDelegated(target);
        }
        return getDecoratee().determinize(target);
    }

    <S extends Symbol> FSA<S> makeCompleteDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> makeComplete(FSA<S> target)
    {
        if (isImplementationTarget(target)) {
            return makeCompleteDelegated(target);
        }
        return getDecoratee().makeComplete(target);
    }

    <S extends Symbol> FSA<S> minimizeDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> minimize(FSA<S> target)
    {
        if (isImplementationTarget(target)) {
            return minimizeDelegated(target);
        }
        return getDecoratee().minimize(target);
    }

    <S extends Symbol> FSA<S> makeComplementDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> makeComplement(FSA<S> target)
    {
        if (isImplementationTarget(target)) {
            return makeComplementDelegated(target);
        }
        return getDecoratee().makeComplement(target);
    }

    default <S extends Symbol> S matchedSymbol(S one, S two)
    {
        return one.equals(two) ? one : null;
    }

    default <S extends Symbol> FSA<S> makeIntersectionDelegated(FSA<S> one, FSA<S> two)
    {
        return makeProductDelegated(one, two, one.getAlphabet(), this::matchedSymbol, (stateMapping, delta) -> {
            final ImmutableList<State> states = stateMapping.keysView().toList().toImmutable();
            final ImmutableBooleanList startStateTable = makeStartStateIntersection(states, stateMapping, one, two);
            final ImmutableBooleanList acceptStateTable = makeAcceptStateIntersection(states, stateMapping, one, two);
            return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
        });
    }

    @Override
    default <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        if (isImplementationTarget(one) && isImplementationTarget(two)) {
            return makeIntersectionDelegated(one, two);
        }
        return getDecoratee().makeIntersection(one, two);
    }

    default <S extends Symbol> FSA<S> makeUnionDelegated(FSA<S> one, FSA<S> two)
    {
        return makeProductDelegated(one, two, one.getAlphabet(), this::matchedSymbol, (stateMapping, delta) -> {
            final ImmutableList<State> states = stateMapping.keysView().toList().toImmutable();
            final ImmutableBooleanList startStateTable = makeStartStateIntersection(states, stateMapping, one, two);
            final ImmutableBooleanList acceptStateTable = makeAcceptStateUnion(states, stateMapping, one, two);
            return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
        });
    }

    @Override
    default <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        if (isImplementationTarget(one) && isImplementationTarget(two)) {
            final FSA<S> completeOne = makeComplete(one);
            final FSA<S> completeTwo = makeComplete(two);
            return makeUnionDelegated(completeOne, completeTwo);
        }
        return getDecoratee().makeUnion(one, two);
    }

    default <S extends Symbol> boolean checkLanguageEmptyDelegated(FSA<S> target)
    {
        return FSAManipulator.super.checkLanguageEmpty(target);
    }

    @Override
    default <S extends Symbol> boolean checkLanguageEmpty(FSA<S> target)
    {
        if (isImplementationTarget(target)) {
            return checkLanguageEmptyDelegated(target);
        }
        return getDecoratee().checkLanguageEmpty(target);
    }

    default <S extends Symbol> boolean checkLanguageSigmaStarDelegated(FSA<S> target)
    {
        return FSAManipulator.super.checkLanguageSigmaStar(target);
    }

    @Override
    default <S extends Symbol> boolean checkLanguageSigmaStar(FSA<S> target)
    {
        if (isImplementationTarget(target)) {
            return checkLanguageSigmaStarDelegated(target);
        }
        return getDecoratee().checkLanguageSigmaStar(target);
    }

    default <S extends Symbol> boolean checkLanguageContainmentDelegated(FSA<S> container, FSA<S> subset)
    {
        return FSAManipulator.super.checkLanguageContainment(container, subset);
    }

    @Override
    default <S extends Symbol> boolean checkLanguageContainment(FSA<S> container, FSA<S> subset)
    {
        if (isImplementationTarget(container) && isImplementationTarget(subset)) {
            return checkLanguageContainmentDelegated(container, subset);
        }
        return getDecoratee().checkLanguageContainment(container, subset);
    }
}
