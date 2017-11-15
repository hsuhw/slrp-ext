package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

import java.util.function.BiFunction;
import java.util.function.Function;

import static api.automata.fsa.FSA.Builder;
import static api.automata.fsa.FSAs.builder;
import static api.util.Connectives.AND;
import static api.util.Connectives.OR;

public interface FSAManipulator extends AutomatonManipulator
{
    default <S> boolean isFSA(Automaton<S> target)
    {
        return target instanceof FSA<?>;
    }

    default <S> FSA<S> trimStates(FSA<S> target, SetIterable<State> toBeTrimmed)
    {
        final Builder<S> builder = FSAs.builder(target);
        toBeTrimmed.forEach(builder::removeState);

        return builder.buildWith(target.alphabet());
    }

    @Override
    <S> FSA<S> trimUnreachableStates(Automaton<S> target);

    @Override
    <S> FSA<S> trimDeadEndStates(Automaton<S> target);

    @Override
    <S> FSA<S> trimDanglingStates(Automaton<S> target);

    @Override
    <S, R> FSA<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector);

    @Override
    <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                             BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer);

    <S> FSA<S> determinize(FSA<S> target);

    default <S> FSA<S> complete(FSA<S> target)
    {
        if (!target.isDeterministic()) {
            throw new IllegalArgumentException("only available on deterministic instances");
        }

        final ImmutableSet<State> incomplete = target.incompleteStates();
        if (incomplete.isEmpty()) {
            return target;
        }

        // complete the ignored transitions of those states
        final Builder<S> builder = FSAs.builder(target);
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

        return builder.buildWith(target.alphabet());
    }

    <S> FSA<S> minimize(FSA<S> target);

    default <S> FSA<S> complement(FSA<S> target)
    {
        final FSA<S> fsa = FSAs.complete(FSAs.determinize(target));

        return FSAs.builder(fsa).resetAcceptStates().addAcceptStates(fsa.nonAcceptStates())
                   .buildWith(target.alphabet());
    }

    private <S> S matchedSymbol(S one, S two)
    {
        return one.equals(two) ? one : null;
    }

    default <S> FSA<S> intersect(FSA<S> one, FSA<S> two)
    {
        return FSAs.product(one, two, one.alphabet(), this::matchedSymbol, (stateMapping, builder) -> {
            final ImmutableSet<State> startStates = AutomatonManipulator
                .selectFrom(stateMapping, one::isStartState, two::isStartState, AND);
            final ImmutableSet<State> acceptStates = AutomatonManipulator
                .selectFrom(stateMapping, one::isAcceptState, two::isAcceptState, AND);
            builder.addStartStates(startStates);
            builder.addAcceptStates(acceptStates);
        });
    }

    default <S> FSA<S> union(FSA<S> one, FSA<S> two)
    {
        final FSA<S> oneFixed = FSAs.complete(determinize(one));
        final FSA<S> twoFixed = FSAs.complete(determinize(two));
        return FSAs.product(oneFixed, twoFixed, oneFixed.alphabet(), this::matchedSymbol, (stateMapping, builder) -> {
            stateMapping.forEachKeyValue((state, statePair) -> {
                final ImmutableSet<State> startStates = AutomatonManipulator
                    .selectFrom(stateMapping, oneFixed::isStartState, twoFixed::isStartState, AND);
                final ImmutableSet<State> acceptStates = AutomatonManipulator
                    .selectFrom(stateMapping, oneFixed::isAcceptState, twoFixed::isAcceptState, OR);
                builder.addStartStates(startStates);
                builder.addAcceptStates(acceptStates);
            });
        });
    }

    <S> LanguageSubsetChecker.Result<S> checkSubset(FSA<S> subsumer, FSA<S> includer);

    interface Decorator extends FSAManipulator
    {
        FSAManipulator decoratee();

        default <S> FSA<S> trimUnreachableStatesImpl(Automaton<S> target)
        {
            if (!isFSA(target)) {
                return null;
            }

            final SetIterable<State> targetStates = target.unreachableStates();

            return targetStates.isEmpty() ? (FSA<S>) target : trimStates((FSA<S>) target, targetStates);
        }

        @Override
        default <S> FSA<S> trimUnreachableStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimUnreachableStatesImpl(target);

            return delegated != null ? delegated : decoratee().trimUnreachableStates(target);
        }

        default <S> FSA<S> trimDeadEndStatesImpl(Automaton<S> target)
        {
            if (!isFSA(target)) {
                return null;
            }
            if (target.acceptStates().isEmpty()) {
                return FSAs.thatAcceptsNone(target.alphabet());
            }

            final SetIterable<State> targetStates = target.deadEndStates();

            return targetStates.isEmpty() ? (FSA<S>) target : trimStates((FSA<S>) target, targetStates);
        }

        @Override
        default <S> FSA<S> trimDeadEndStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimDeadEndStatesImpl(target);

            return delegated != null ? delegated : decoratee().trimDeadEndStates(target);
        }

        default <S> FSA<S> trimDanglingStatesImpl(Automaton<S> target)
        {
            if (!isFSA(target)) {
                return null;
            }
            if (target.acceptStates().isEmpty()) {
                return FSAs.thatAcceptsNone(target.alphabet());
            }

            final SetIterable<State> targetStates = target.danglingStates();

            return targetStates.isEmpty() ? (FSA<S>) target : trimStates((FSA<S>) target, targetStates);
        }

        @Override
        default <S> FSA<S> trimDanglingStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimDanglingStatesImpl(target);

            return delegated != null ? delegated : decoratee().trimDanglingStates(target);
        }

        default <S, R> FSA<R> projectImpl(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector)
        {
            if (!isFSA(target)) {
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

            return FSAs.trimDanglingStates(builder.buildWith(alphabet));
        }

        @Override
        default <S, R> FSA<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector)
        {
            final FSA<R> delegated = projectImpl(target, alphabet, projector);

            return delegated != null ? delegated : decoratee().project(target, alphabet, projector);
        }

        default <S, T, R> FSA<R> productImpl(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                             BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
        {
            return null;
        }

        @Override
        default <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                         BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer)
        {
            final FSA<R> delegated = productImpl(one, two, alphabet, transitionDecider, finalizer);

            return delegated != null
                   ? delegated
                   : decoratee().product(one, two, alphabet, transitionDecider, finalizer);
        }

        default <S> FSA<S> determinizeImpl(FSA<S> target)
        {
            return null;
        }

        @Override
        default <S> FSA<S> determinize(FSA<S> target)
        {
            final FSA<S> delegated = determinizeImpl(target);

            return delegated != null ? delegated : decoratee().determinize(target);
        }

        default <S> FSA<S> completeImpl(FSA<S> target)
        {
            return FSAManipulator.super.complete(target);
        }

        @Override
        default <S> FSA<S> complete(FSA<S> target)
        {
            final FSA<S> delegated = completeImpl(target);

            return delegated != null ? delegated : decoratee().complement(target);
        }

        default <S> FSA<S> minimizeImpl(FSA<S> target)
        {
            return null;
        }

        @Override
        default <S> FSA<S> minimize(FSA<S> target)
        {
            final FSA<S> delegated = minimizeImpl(target);

            return delegated != null ? delegated : decoratee().minimize(target);
        }

        default <S> FSA<S> complementImpl(FSA<S> target)
        {
            return FSAManipulator.super.complement(target);
        }

        @Override
        default <S> FSA<S> complement(FSA<S> target)
        {
            final FSA<S> delegated = complementImpl(target);

            return delegated != null ? delegated : decoratee().complement(target);
        }

        default <S> FSA<S> intersectImpl(FSA<S> one, FSA<S> two)
        {
            return FSAManipulator.super.intersect(one, two);
        }

        @Override
        default <S> FSA<S> intersect(FSA<S> one, FSA<S> two)
        {
            final FSA<S> delegated = intersectImpl(one, two);

            return delegated != null ? delegated : decoratee().intersect(one, two);
        }

        default <S> FSA<S> unionImpl(FSA<S> one, FSA<S> two)
        {
            return FSAManipulator.super.union(one, two);
        }

        @Override
        default <S> FSA<S> union(FSA<S> one, FSA<S> two)
        {
            final FSA<S> delegated = unionImpl(one, two);

            return delegated != null ? delegated : decoratee().union(one, two);
        }

        default <S> LanguageSubsetChecker.Result<S> checkSubsetImpl(FSA<S> subsumer, FSA<S> includer)
        {
            return null;
        }

        @Override
        default <S> LanguageSubsetChecker.Result<S> checkSubset(FSA<S> subsumer, FSA<S> includer)
        {
            final LanguageSubsetChecker.Result<S> delegated = checkSubsetImpl(subsumer, includer);

            return delegated != null ? delegated : decoratee().checkSubset(subsumer, includer);
        }
    }
}
