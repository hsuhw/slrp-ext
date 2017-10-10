package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

import java.util.function.BiFunction;

import static api.util.Connectives.AND;
import static api.util.Connectives.OR;

public interface FSAManipulator extends AutomatonManipulator
{
    @Override
    <S> FSA<S> trimUnreachableStates(Automaton<S> target);

    @Override
    <S> FSA<S> trimDeadEndStates(Automaton<S> target);

    @Override
    <S, T, R> FSA<R> makeProduct(Automaton<S> one, Automaton<T> two, Alphabet<R> targetAlphabet,
                                 BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer);

    <S> FSA<S> determinize(FSA<S> fsa);

    default <S> FSA<S> makeComplete(FSA<S> target)
    {
        if (target instanceof Nondeterministic) {
            throw new IllegalArgumentException("only available on deterministic instances");
        }

        final SetIterable<State> incomplete = target.getIncompleteStates();
        if (incomplete.isEmpty()) {
            return target;
        }

        // complete the ignored transitions of those states
        final FSA.Builder<S> builder = FSAs.builderBasedOn(target);
        final State deadEndState = States.generate();
        final ImmutableSet<S> completeAlphabet = target.getAlphabet().noEpsilonSet();
        completeAlphabet.forEach(symbol -> {
            builder.addTransition(deadEndState, deadEndState, symbol);
        });
        final DeltaFunction<S> delta = target.getDeltaFunction();
        incomplete.forEach(state -> {
            completeAlphabet.newWithoutAll(delta.enabledSymbolsOn(state)).forEach(symbol -> {
                builder.addTransition(state, deadEndState, symbol);
            });
        });

        return builder.build();
    }

    <S> FSA<S> minimize(FSA<S> target);

    default <S> FSA<S> makeComplement(FSA<S> target)
    {
        final FSA<S> fsa = makeComplete(determinize(target));

        return FSAs.builderBasedOn(fsa).resetAcceptStates().addAcceptStates(fsa.getNonAcceptStates()).build();
    }

    private <S> S matchedSymbol(S one, S two)
    {
        return one.equals(two) ? one : null;
    }

    default <S> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        return makeProduct(one, two, one.getAlphabet(), this::matchedSymbol, (stateMapping, builder) -> {
            final SetIterable<State> startStates = AutomatonManipulator
                .selectStatesFromProduct(stateMapping, one::isStartState, two::isStartState, AND);
            final SetIterable<State> acceptStates = AutomatonManipulator
                .selectStatesFromProduct(stateMapping, one::isAcceptState, two::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });
    }

    default <S> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        return makeProduct(one, two, one.getAlphabet(), this::matchedSymbol, (stateMapping, builder) -> {
            stateMapping.forEachKeyValue((state, statePair) -> {
                final SetIterable<State> startStates = AutomatonManipulator
                    .selectStatesFromProduct(stateMapping, one::isStartState, two::isStartState, AND);
                final SetIterable<State> acceptStates = AutomatonManipulator
                    .selectStatesFromProduct(stateMapping, one::isAcceptState, two::isAcceptState, OR);
                builder.addStartStates(startStates);
                builder.addAcceptStates(acceptStates);
            });
        });
    }

    default <S> boolean checkLanguageEmpty(FSA<S> target)
    {
        return trimUnreachableStates(target).getAcceptStates().size() == 0;
    }

    default <S> boolean checkLanguageSigmaStar(FSA<S> target)
    {
        return checkLanguageEmpty(makeComplement(target));
    }

    default <S> boolean checkLanguageContainment(FSA<S> container, FSA<S> subset)
    {
        return checkLanguageEmpty(makeIntersection(container, makeComplement(subset)));
    }

    interface Decorator extends FSAManipulator
    {
        FSAManipulator getDecoratee();

        <S> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target);

        @Override
        default <S> FSA<S> trimUnreachableStates(Automaton<S> fsa)
        {
            final FSA<S> delegated = trimUnreachableStatesDelegated(fsa);
            if (delegated == null) {
                return getDecoratee().trimUnreachableStates(fsa);
            }
            return delegated;
        }

        <S> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target);

        @Override
        default <S> FSA<S> trimDeadEndStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimDeadEndStatesDelegated(target);
            if (delegated == null) {
                return getDecoratee().trimDeadEndStates(target);
            }
            return delegated;
        }

        <S, T, R> FSA<R> makeProductDelegated(Automaton<S> one, Automaton<T> two, Alphabet<R> targetAlphabet,
                                              BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer);

        @Override
        default <S, T, R> FSA<R> makeProduct(Automaton<S> one, Automaton<T> two, Alphabet<R> targetAlphabet,
                                             BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
        {
            final FSA<R> delegated = makeProductDelegated(one, two, targetAlphabet, transitionDecider, finalizer);
            if (delegated == null) {
                return getDecoratee().makeProduct(one, two, targetAlphabet, transitionDecider, finalizer);
            }
            return delegated;
        }

        <S> FSA<S> determinizeDelegated(FSA<S> target);

        @Override
        default <S> FSA<S> determinize(FSA<S> target)
        {
            final FSA<S> delegated = determinizeDelegated(target);
            if (delegated == null) {
                return getDecoratee().determinize(target);
            }
            return delegated;
        }

        default <S> FSA<S> makeCompleteDelegated(FSA<S> target)
        {
            return FSAManipulator.super.makeComplete(target);
        }

        @Override
        default <S> FSA<S> makeComplete(FSA<S> target)
        {
            final FSA<S> delegated = makeCompleteDelegated(target);
            if (delegated == null) {
                return getDecoratee().makeComplete(target);
            }
            return delegated;
        }

        <S> FSA<S> minimizeDelegated(FSA<S> target);

        @Override
        default <S> FSA<S> minimize(FSA<S> target)
        {
            final FSA<S> delegated = minimizeDelegated(target);
            if (delegated == null) {
                return getDecoratee().minimize(target);
            }
            return delegated;
        }

        default <S> FSA<S> makeComplementDelegated(FSA<S> target)
        {
            return FSAManipulator.super.makeComplement(target);
        }

        @Override
        default <S> FSA<S> makeComplement(FSA<S> target)
        {
            final FSA<S> delegated = makeComplementDelegated(target);
            if (delegated == null) {
                return getDecoratee().makeComplement(target);
            }
            return delegated;
        }

        default <S> FSA<S> makeIntersectionDelegated(FSA<S> one, FSA<S> two)
        {
            return FSAManipulator.super.makeIntersection(one, two);
        }

        @Override
        default <S> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
        {
            final FSA<S> delegated = makeIntersectionDelegated(one, two);
            if (delegated == null) {
                return getDecoratee().makeIntersection(one, two);
            }
            return delegated;
        }

        default <S> FSA<S> makeUnionDelegated(FSA<S> one, FSA<S> two)
        {
            return FSAManipulator.super.makeUnion(one, two);
        }

        @Override
        default <S> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
        {
            final FSA<S> completeOne = makeComplete(one);
            final FSA<S> completeTwo = makeComplete(two);
            final FSA<S> delegated = makeUnionDelegated(completeOne, completeTwo);
            if (delegated == null) {
                return getDecoratee().makeUnion(one, two);
            }
            return delegated;
        }

        default <S> Boolean checkLanguageEmptyDelegated(FSA<S> target)
        {
            return FSAManipulator.super.checkLanguageEmpty(target);
        }

        @Override
        default <S> boolean checkLanguageEmpty(FSA<S> target)
        {
            final Boolean delegated = checkLanguageEmptyDelegated(target);
            if (delegated == null) {
                return getDecoratee().checkLanguageEmpty(target);
            }
            return delegated;
        }

        default <S> Boolean checkLanguageSigmaStarDelegated(FSA<S> target)
        {
            return FSAManipulator.super.checkLanguageSigmaStar(target);
        }

        @Override
        default <S> boolean checkLanguageSigmaStar(FSA<S> target)
        {
            final Boolean delegated = checkLanguageSigmaStarDelegated(target);
            if (delegated == null) {
                return getDecoratee().checkLanguageSigmaStar(target);
            }
            return delegated;
        }

        default <S> Boolean checkLanguageContainmentDelegated(FSA<S> container, FSA<S> subset)
        {
            return FSAManipulator.super.checkLanguageContainment(container, subset);
        }

        @Override
        default <S> boolean checkLanguageContainment(FSA<S> container, FSA<S> subset)
        {
            final Boolean delegated = checkLanguageContainmentDelegated(container, subset);
            if (delegated == null) {
                return getDecoratee().checkLanguageContainment(container, subset);
            }
            return delegated;
        }
    }
}
