package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

import java.util.function.BiFunction;
import java.util.function.Function;

import static api.automata.fsa.FSA.Builder;
import static api.automata.fsa.FSAs.*;
import static api.util.Connectives.AND;
import static api.util.Connectives.OR;

public interface FSAManipulator extends AutomatonManipulator
{
    @Override
    <S> FSA<S> trimUnreachableStates(Automaton<S> target);

    @Override
    <S> FSA<S> trimDeadEndStates(Automaton<S> target);

    @Override
    default <S> FSA<S> trimStates(Automaton<S> target)
    {
        return manipulator().trimUnreachableStates(manipulator().trimDeadEndStates(target));
    }

    @Override
    <S, R> FSA<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector);

    @Override
    <S, T, R> FSA<R> makeProduct(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                 BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer);

    <S> FSA<S> determinize(FSA<S> fsa);

    default <S> FSA<S> makeComplete(FSA<S> target)
    {
        if (!target.isDeterministic()) {
            throw new IllegalArgumentException("only available on deterministic instances");
        }

        final ImmutableSet<State> incomplete = target.incompleteStates();
        if (incomplete.isEmpty()) {
            return target;
        }

        // complete the ignored transitions of those states
        final Builder<S> builder = builderOn(target);
        final State deadEndState = States.generate();
        final ImmutableSet<S> completeAlphabet = target.alphabet().noEpsilonSet();
        completeAlphabet.forEach(symbol -> {
            builder.addTransition(deadEndState, deadEndState, symbol);
        });
        final TransitionGraph<State, S> delta = target.transitionGraph();
        incomplete.forEach(state -> {
            completeAlphabet.newWithoutAll(delta.enabledArcsOn(state)).forEach(symbol -> {
                builder.addTransition(state, deadEndState, symbol);
            });
        });

        return builder.build();
    }

    <S> FSA<S> minimize(FSA<S> target);

    default <S> FSA<S> makeComplement(FSA<S> target)
    {
        final FSA<S> fsa = manipulator().makeComplete(manipulator().determinize(target));

        return builderOn(fsa).resetAcceptStates().addAcceptStates(fsa.nonAcceptStates()).build();
    }

    private <S> S matchedSymbol(S one, S two)
    {
        return one.equals(two) ? one : null;
    }

    default <S> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        return manipulator().makeProduct(one, two, one.alphabet(), this::matchedSymbol, (stateMapping, builder) -> {
            final ImmutableSet<State> startStates = AutomatonManipulator
                .selectFromProduct(stateMapping, one::isStartState, two::isStartState, AND);
            final ImmutableSet<State> acceptStates = AutomatonManipulator
                .selectFromProduct(stateMapping, one::isAcceptState, two::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });
    }

    default <S> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        final FSA<S> oneFixed = manipulator().makeComplete(determinize(one));
        final FSA<S> twoFixed = manipulator().makeComplete(determinize(two));
        return manipulator()
            .makeProduct(oneFixed, twoFixed, oneFixed.alphabet(), this::matchedSymbol, (stateMapping, builder) -> {
                stateMapping.forEachKeyValue((state, statePair) -> {
                    final ImmutableSet<State> startStates = AutomatonManipulator
                        .selectFromProduct(stateMapping, oneFixed::isStartState, twoFixed::isStartState, AND);
                    final ImmutableSet<State> acceptStates = AutomatonManipulator
                        .selectFromProduct(stateMapping, oneFixed::isAcceptState, twoFixed::isAcceptState, OR);
                    builder.addStartStates(startStates);
                    builder.addAcceptStates(acceptStates);
                });
            });
    }

    default <S> boolean checkAcceptingNone(FSA<S> target)
    {
        return manipulator().trimUnreachableStates(target).acceptStates().size() == 0;
    }

    default <S> boolean checkAcceptingAll(FSA<S> target)
    {
        return manipulator().checkAcceptingNone(manipulator().makeComplement(target));
    }

    default <S> boolean checkLanguageSubset(FSA<S> toInclude, FSA<S> toSubsume)
    {
        if (manipulator().checkAcceptingNone(toSubsume)) {
            return true;
        }
        if (manipulator().checkAcceptingNone(toInclude)) {
            return false;
        }

        final FSA<S> toIncludeBar = manipulator().makeComplement(toInclude);

        return manipulator().checkAcceptingNone(toIncludeBar) //
            || manipulator().checkAcceptingNone(manipulator().makeIntersection(toIncludeBar, toSubsume));
    }

    default <S> ImmutableList<S> witnessLanguageNotSubset(FSA<S> toInclude, FSA<S> toSubsume)
    {
        if (manipulator().checkAcceptingNone(toSubsume)) {
            return null;
        }
        if (manipulator().checkAcceptingNone(toInclude)) {
            return toSubsume.enumerateOneShortestWord();
        }

        final FSA<S> toIncludeBar = manipulator().makeComplement(toInclude);
        if (manipulator().checkAcceptingNone(toIncludeBar)) {
            return null;
        }

        return manipulator().makeIntersection(toIncludeBar, toSubsume).enumerateOneShortestWord();
    }

    interface Decorator extends FSAManipulator
    {
        FSAManipulator getDecoratee();

        default <S> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target)
        {
            return null;
        }

        @Override
        default <S> FSA<S> trimUnreachableStates(Automaton<S> fsa)
        {
            final FSA<S> delegated = trimUnreachableStatesDelegated(fsa);
            if (delegated == null) {
                return getDecoratee().trimUnreachableStates(fsa);
            }
            return delegated;
        }

        default <S> FSA<S> trimDeadEndStatesDelegated(Automaton<S> target)
        {
            return null;
        }

        @Override
        default <S> FSA<S> trimDeadEndStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimDeadEndStatesDelegated(target);
            if (delegated == null) {
                return getDecoratee().trimDeadEndStates(target);
            }
            return delegated;
        }

        default <S> FSA<S> trimStatesDelegated(Automaton<S> target)
        {
            return FSAManipulator.super.trimStates(target);
        }

        @Override
        default <S> FSA<S> trimStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimStatesDelegated(target);
            if (delegated == null) {
                return getDecoratee().trimStates(target);
            }
            return delegated;
        }

        default <S, R> FSA<R> projectDelegated(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector)
        {
            if (!(target instanceof FSA<?>)) {
                return null;
            }

            final Builder<R> builder = builder(target.states().size(), target.alphabet().size(), alphabet.epsilon());
            builder.addStartStates(target.startStates());
            builder.addAcceptStates(target.acceptStates());
            final TransitionGraph<State, S> delta = target.transitionGraph();
            R newSymbol;
            for (State dept : target.states()) {
                for (S symbol : delta.enabledArcsOn(dept)) {
                    for (State dest : delta.successorsOf(dept, symbol)) {
                        if ((newSymbol = projector.apply(symbol)) != null) {
                            builder.addTransition(dept, dest, newSymbol);
                        }
                    }
                }
            }

            return manipulator().trimStates(builder.build(alphabet));
        }

        @Override
        default <S, R> FSA<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector)
        {
            final FSA<R> delegated = projectDelegated(target, alphabet, projector);
            if (delegated == null) {
                return getDecoratee().project(target, alphabet, projector);
            }
            return delegated;
        }

        default <S, T, R> FSA<R> makeProductDelegated(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                                      BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
        {
            return null;
        }

        @Override
        default <S, T, R> FSA<R> makeProduct(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                             BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
        {
            final FSA<R> delegated = makeProductDelegated(one, two, alphabet, transitionDecider, finalizer);
            if (delegated == null) {
                return getDecoratee().makeProduct(one, two, alphabet, transitionDecider, finalizer);
            }
            return delegated;
        }

        default <S> FSA<S> determinizeDelegated(FSA<S> target)
        {
            return null;
        }

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

        default <S> FSA<S> minimizeDelegated(FSA<S> target)
        {
            return null;
        }

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
            final FSA<S> delegated = makeUnionDelegated(one, two);
            if (delegated == null) {
                return getDecoratee().makeUnion(one, two);
            }
            return delegated;
        }

        default <S> Boolean checkAcceptingNoneDelegated(FSA<S> target)
        {
            return FSAManipulator.super.checkAcceptingNone(target);
        }

        @Override
        default <S> boolean checkAcceptingNone(FSA<S> target)
        {
            final Boolean delegated = checkAcceptingNoneDelegated(target);
            if (delegated == null) {
                return getDecoratee().checkAcceptingNone(target);
            }
            return delegated;
        }

        default <S> Boolean checkAcceptingAllDelegated(FSA<S> target)
        {
            return FSAManipulator.super.checkAcceptingAll(target);
        }

        @Override
        default <S> boolean checkAcceptingAll(FSA<S> target)
        {
            final Boolean delegated = checkAcceptingAllDelegated(target);
            if (delegated == null) {
                return getDecoratee().checkAcceptingAll(target);
            }
            return delegated;
        }

        default <S> Boolean checkLanguageSubsetDelegated(FSA<S> toInclude, FSA<S> toSubsume)
        {
            return FSAManipulator.super.checkLanguageSubset(toInclude, toSubsume);
        }

        @Override
        default <S> boolean checkLanguageSubset(FSA<S> toInclude, FSA<S> toSubsume)
        {
            final Boolean delegated = checkLanguageSubsetDelegated(toInclude, toSubsume);
            if (delegated == null) {
                return getDecoratee().checkLanguageSubset(toInclude, toSubsume);
            }
            return delegated;
        }

        default <S> ImmutableList<S> witnessLanguageNotSubsetDelegated(FSA<S> toInclude, FSA<S> toSubsume)
        {
            return FSAManipulator.super.witnessLanguageNotSubset(toInclude, toSubsume);
        }

        @Override
        default <S> ImmutableList<S> witnessLanguageNotSubset(FSA<S> toInclude, FSA<S> toSubsume)
        {
            final ImmutableList<S> delegated = witnessLanguageNotSubsetDelegated(toInclude, toSubsume);
            if (delegated == null) {
                return getDecoratee().witnessLanguageNotSubset(toInclude, toSubsume);
            }
            return delegated;
        }
    }
}
