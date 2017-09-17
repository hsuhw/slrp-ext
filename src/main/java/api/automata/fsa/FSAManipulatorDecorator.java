package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.Symbol;

import java.util.function.BiFunction;

public interface FSAManipulatorDecorator extends FSAManipulator
{
    FSAManipulator getDecoratee();

    <S extends Symbol> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target);

    @Override
    default <S extends Symbol> FSA<S> trimUnreachableStates(Automaton<S> target)
    {
        final FSA<S> delegated = trimUnreachableStatesDelegated(target);
        if (delegated == null) {
            return getDecoratee().trimUnreachableStates(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target);

    @Override
    default <S extends Symbol> FSA<S> trimDeadEndStates(Automaton<S> target)
    {
        final FSA<S> delegated = trimDeadEndStatesDelegated(target);
        if (delegated == null) {
            return getDecoratee().trimDeadEndStates(target);
        }
        return delegated;
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
        final FSA<R> delegated = makeProductDelegated(one, two, targetAlphabet, transitionDecider,
                                                      stateAttributeDecider);
        if (delegated == null) {
            return getDecoratee().makeProduct(one, two, targetAlphabet, transitionDecider, stateAttributeDecider);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> determinize(FSA<S> target)
    {
        final FSA<S> delegated = determinizeDelegated(target);
        if (delegated == null) {
            return getDecoratee().determinize(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> makeCompleteDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> makeComplete(FSA<S> target)
    {
        final FSA<S> delegated = makeCompleteDelegated(target);
        if (delegated == null) {
            return getDecoratee().makeComplete(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> minimizeDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> minimize(FSA<S> target)
    {
        final FSA<S> delegated = minimizeDelegated(target);
        if (delegated == null) {
            return getDecoratee().minimize(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> makeComplementDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> makeComplement(FSA<S> target)
    {
        final FSA<S> delegated = makeComplementDelegated(target);
        if (delegated == null) {
            return getDecoratee().makeComplement(target);
        }
        return delegated;
    }

    default <S extends Symbol> FSA<S> makeIntersectionDelegated(FSA<S> one, FSA<S> two)
    {
        return FSAManipulator.super.makeIntersection(one, two);
    }

    @Override
    default <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        final FSA<S> delegated = makeIntersectionDelegated(one, two);
        if (delegated == null) {
            return getDecoratee().makeIntersection(one, two);
        }
        return delegated;
    }

    default <S extends Symbol> FSA<S> makeUnionDelegated(FSA<S> one, FSA<S> two)
    {
        return FSAManipulator.super.makeUnion(one, two);
    }

    @Override
    default <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        final FSA<S> completeOne = makeComplete(one);
        final FSA<S> completeTwo = makeComplete(two);
        final FSA<S> delegated = makeUnionDelegated(completeOne, completeTwo);
        if (delegated == null) {
            return getDecoratee().makeUnion(one, two);
        }
        return delegated;
    }

    default <S extends Symbol> Boolean checkLanguageEmptyDelegated(FSA<S> target)
    {
        return FSAManipulator.super.checkLanguageEmpty(target);
    }

    @Override
    default <S extends Symbol> boolean checkLanguageEmpty(FSA<S> target)
    {
        final Boolean delegated = checkLanguageEmptyDelegated(target);
        if (delegated == null) {
            return getDecoratee().checkLanguageEmpty(target);
        }
        return delegated;
    }

    default <S extends Symbol> Boolean checkLanguageSigmaStarDelegated(FSA<S> target)
    {
        return FSAManipulator.super.checkLanguageSigmaStar(target);
    }

    @Override
    default <S extends Symbol> boolean checkLanguageSigmaStar(FSA<S> target)
    {
        final Boolean delegated = checkLanguageSigmaStarDelegated(target);
        if (delegated == null) {
            return getDecoratee().checkLanguageSigmaStar(target);
        }
        return delegated;
    }

    default <S extends Symbol> Boolean checkLanguageContainmentDelegated(FSA<S> container, FSA<S> subset)
    {
        return FSAManipulator.super.checkLanguageContainment(container, subset);
    }

    @Override
    default <S extends Symbol> boolean checkLanguageContainment(FSA<S> container, FSA<S> subset)
    {
        final Boolean delegated = checkLanguageContainmentDelegated(container, subset);
        if (delegated == null) {
            return getDecoratee().checkLanguageContainment(container, subset);
        }
        return delegated;
    }
}
