package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.sorted.mutable.TreeSortedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

import static api.automata.AutomatonManipulator.makeStartAndAcceptStates;
import static api.automata.fsa.FSA.Builder;
import static api.automata.fsa.FSAs.builder;
import static api.util.Connectives.AND;
import static api.util.Connectives.Labels;

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
                             SymbolDecider<S, T, R> symbolDecider, Finalizer<R> finalizer);

    @Override
    <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet, StepFilter<S, T, R> stepFilter,
                             Finalizer<R> finalizer);

    default <S> FSA<S> determinize(FSA<S> target)
    {
        if (target.isDeterministic()) {
            return target;
        }

        final Alphabet<S> alphabet = target.alphabet();
        final TransitionGraph<State, S> delta = target.transitionGraph();
        final int capacity = target.states().size() * target.states().size(); // heuristic
        final Builder<S> builder = builder(capacity, alphabet.size(), alphabet.epsilon());
        final MutableBiMap<MutableSet<State>, State> stateMapping = new HashBiMap<>(capacity);
        final Queue<MutableSet<State>> pendingStateSets = new LinkedList<>();

        final MutableSet<State> startStates = delta.epsilonClosureOf(target.startStates()).toSet();
        final State newStart = States.generate();
        stateMapping.put(startStates, newStart);
        builder.addStartState(newStart);
        pendingStateSets.add(startStates);
        final SetIterable<S> noEpsilonSymbolSet = alphabet.noEpsilonSet();
        MutableSet<State> currStateSet;
        while ((currStateSet = pendingStateSets.poll()) != null) {
            final State newDept = stateMapping.get(currStateSet);
            for (S symbol : noEpsilonSymbolSet) {
                final MutableSet<State> destStates = delta.epsilonClosureOf(currStateSet, symbol).toSet();
                final State newDest = stateMapping.computeIfAbsent(destStates, __ -> {
                    pendingStateSets.add(destStates);
                    final State s = States.generate();
                    if (destStates.anySatisfy(target::isAcceptState)) {
                        builder.addAcceptState(s);
                    }
                    return s;
                });
                builder.addTransition(newDept, newDest, symbol);
            }
        }

        return builder.buildWith(alphabet);
    }

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
            completeAlphabet.newWithoutAll(delta.arcLabelsFrom(state)).forEach(symbol -> {
                builder.addTransition(state, deadEndState, symbol);
            });
        });

        return builder.buildWith(target.alphabet());
    }

    default <S> Twin<ImmutableList<State>> splitPartition(TransitionGraph<State, S> delta, ImmutableList<State> part,
                                                          Pair<ImmutableList<State>, S> splitter)
    {
        final S symbol = splitter.getTwo();
        final ImmutableList<State> filter = splitter.getOne();
        final ImmutableList<State> split1 = part
            .select(state -> filter.contains(delta.directSuccessorOf(state, symbol)));
        final ImmutableList<State> split2 = part.newWithoutAll(split1);

        return split1.size() < split2.size() ? Tuples.twin(split1, split2) : Tuples.twin(split2, split1);
    }

    default <S> List<ImmutableList<State>> refinePartition(TransitionGraph<State, S> delta, ImmutableSet<S> alphabet,
                                                           ImmutableList<ImmutableList<State>> initialPartition,
                                                           ImmutableList<State> initialCheck)
    {
        final MutableSortedSet<Pair<ImmutableList<State>, S>> pendingChecks = TreeSortedSet
            .newSet(Comparator.comparingInt(Object::hashCode));
        alphabet.forEach(symbol -> pendingChecks.add(Tuples.pair(initialCheck, symbol)));

        ImmutableList<ImmutableList<State>> currentPartition = initialPartition;
        while (pendingChecks.notEmpty()) {
            final Pair<ImmutableList<State>, S> currCheck = pendingChecks.getFirst();
            pendingChecks.remove(currCheck);
            currentPartition = currentPartition.flatCollect(part -> {
                final Twin<ImmutableList<State>> parts = splitPartition(delta, part, currCheck);
                if (parts.getOne().notEmpty()) {
                    alphabet.forEach(symbol -> {
                        final Pair<ImmutableList<State>, S> splitCheck = Tuples.pair(part, symbol);
                        if (pendingChecks.contains(splitCheck)) {
                            pendingChecks.remove(splitCheck);
                            pendingChecks.add(Tuples.pair(parts.getOne(), symbol));
                            pendingChecks.add(Tuples.pair(parts.getTwo(), symbol));
                        } else {
                            pendingChecks.add(Tuples.pair(parts.getOne(), symbol));
                        }
                    });
                    return Lists.immutable.of(parts.getOne(), parts.getTwo());
                }
                return Lists.immutable.of(part);
            });
        }

        return currentPartition.castToList();
    }

    private <S> FSA<S> minimize(FSA<S> origin, List<ImmutableList<State>> statePartition)
    {
        final Alphabet<S> alphabet = origin.alphabet();
        final ImmutableSet<S> symbols = alphabet.noEpsilonSet();
        final TransitionGraph<State, S> delta = origin.transitionGraph();
        final MutableMap<ImmutableList<State>, State> setToNewState = UnifiedMap.newMap(statePartition.size());
        statePartition.forEach(part -> setToNewState.put(part, States.generate()));
        final MutableMap<State, State> stateToNewState = UnifiedMap.newMap(origin.states().size());
        statePartition.forEach(part -> part.forEach(state -> stateToNewState.put(state, setToNewState.get(part))));
        final FSA.Builder<S> builder = FSAs.builder(statePartition.size(), alphabet.size(), alphabet.epsilon());

        statePartition.forEach(part -> {
            final State state = setToNewState.get(part);
            if (part.anySatisfy(origin::isStartState)) {
                builder.addStartState(state);
            }
            if (part.anySatisfy(origin::isAcceptState)) {
                builder.addAcceptState(state);
            }
            symbols.forEach(symbol -> {
                final State dest = stateToNewState.get(delta.directSuccessorOf(part.getFirst(), symbol));
                builder.addTransition(state, dest, symbol);
            });
        });

        return builder.buildWith(alphabet);
    }

    default <S> FSA<S> minimize(FSA<S> target)
    {
        if (!target.isDeterministic()) {
            throw new IllegalArgumentException("only available on deterministic instances");
        }
        final Alphabet<S> alphabet = target.alphabet();
        if (target.acceptsNone()) {
            return FSAs.thatAcceptsNone(alphabet);
        }

        final FSA<S> dfa = FSAs.complete(target);
        final TransitionGraph<State, S> delta = dfa.transitionGraph();
        final ImmutableList<State> accepts = dfa.acceptStates().toList().toImmutable();
        final ImmutableList<State> nonAccepts = dfa.nonAcceptStates().toList().toImmutable();
        final ImmutableList<ImmutableList<State>> initialPartition = Lists.immutable.of(accepts, nonAccepts);
        final ImmutableList<State> initialCheck = accepts.size() < nonAccepts.size() ? accepts : nonAccepts;

        return minimize(dfa, refinePartition(delta, alphabet.noEpsilonSet(), initialPartition, initialCheck));
    }

    default <S> FSA<S> complement(FSA<S> target)
    {
        final FSA<S> fsa = FSAs.complete(FSAs.determinize(target));

        return FSAs.builder(fsa).resetAcceptStates().addAcceptStates(fsa.nonAcceptStates())
                   .buildWith(target.alphabet());
    }

    default <S> FSA<S> intersect(FSA<S> one, FSA<S> two)
    {
        return FSAs.product(one, two, one.alphabet(), Labels.matched(), makeStartAndAcceptStates(one, two, AND, AND));
    }

    default <S> FSA<S> union(FSA<S> one, FSA<S> two)
    {
        final boolean oneBigger = one.states().size() > two.states().size();
        final FSA<S> bigger = oneBigger ? one : two;
        final FSA<S> smaller = oneBigger ? two : one;
        final int stateCapacity = one.states().size() + two.states().size();
        final int transitionCapacity = one.transitionGraph().size() + two.transitionGraph().size();
        final FSA.Builder<S> builder = FSAs.builder(bigger, stateCapacity, transitionCapacity);

        builder.addTransitions(smaller.transitionGraph());
        builder.addAcceptStates(smaller.acceptStates());
        final S epsilonSymbol = bigger.alphabet().epsilon();
        final State newStartState = States.generate();
        builder.resetStartStates();
        builder.addStartState(newStartState);
        bigger.startStates().forEach(state -> builder.addTransition(newStartState, state, epsilonSymbol));
        smaller.startStates().forEach(state -> builder.addTransition(newStartState, state, epsilonSymbol));

        return builder.buildWith(bigger.alphabet());
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
                for (S symbol : delta.arcLabelsFrom(dept)) {
                    for (State dest : delta.directSuccessorsOf(dept, symbol)) {
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
                                             SymbolDecider<S, T, R> decider, Finalizer<R> finalizer)
        {
            return null;
        }

        @Override
        default <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                         SymbolDecider<S, T, R> symbolDecider, Finalizer<R> finalizer)
        {
            final FSA<R> delegated = productImpl(one, two, alphabet, symbolDecider, finalizer);

            return delegated != null ? delegated : decoratee().product(one, two, alphabet, symbolDecider, finalizer);
        }

        default <S, T, R> FSA<R> productImpl(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                             StepFilter<S, T, R> stepFilter, Finalizer<R> finalizer)
        {
            return null;
        }

        @Override
        default <S, T, R> FSA<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                         StepFilter<S, T, R> stepFilter, Finalizer<R> finalizer)
        {
            final FSA<R> delegated = productImpl(one, two, alphabet, stepFilter, finalizer);

            return delegated != null ? delegated : decoratee().product(one, two, alphabet, stepFilter, finalizer);
        }

        default <S> FSA<S> determinizeImpl(FSA<S> target)
        {
            return FSAManipulator.super.determinize(target);
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
            return FSAManipulator.super.minimize(target);
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
