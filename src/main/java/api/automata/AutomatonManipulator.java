package api.automata;

import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.function.Function;
import java.util.function.Predicate;

import static api.automata.Automaton.Builder;

public interface AutomatonManipulator
{
    static ImmutableSet<State> selectFrom(BiMap<Twin<State>, State> stateMapping, Predicate<State> filter1,
                                          BooleanBooleanPredicate connective, Predicate<State> filter2)
    {
        final MutableSet<State> result = UnifiedSet.newSet(stateMapping.size()); // upper bound
        stateMapping.forEachKeyValue((statePair, state) -> {
            if (connective.accept(filter1.test(statePair.getOne()), filter2.test(statePair.getTwo()))) {
                result.add(state);
            }
        });

        return result.toImmutable();
    }

    static <S, T, R> Finalizer<R> makeStartAndAcceptStates(Automaton<S> one, Automaton<T> two,
                                                           BooleanBooleanPredicate startCombinator,
                                                           BooleanBooleanPredicate acceptCombinator)
    {
        return (stateMapping, builder) -> {
            builder.addStartStates(selectFrom(stateMapping, one::isStartState, startCombinator, two::isStartState));
            builder.addAcceptStates(selectFrom(stateMapping, one::isAcceptState, acceptCombinator, two::isAcceptState));
        };
    }

    <S> Automaton<S> trimUnreachableStates(Automaton<S> target);

    <S> Automaton<S> trimDeadEndStates(Automaton<S> target);

    <S> Automaton<S> trimDanglingStates(Automaton<S> target);

    <S, R> Automaton<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector);

    <S, T, R> Automaton<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                   SymbolDecider<S, T, R> symbolDecider, Finalizer<R> finalizer);

    <S, T, R> Automaton<R> product(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                   StepFilter<S, T, R> stepFilter, Finalizer<R> finalizer);

    @FunctionalInterface
    interface SymbolDecider<S, T, R>
    {
        R apply(S symbol1, T symbol2);
    }

    @FunctionalInterface
    interface StepFilter<S, T, R>
    {
        R apply(Twin<State> depts, S symbol1, T symbol2);
    }

    @FunctionalInterface
    interface StepDecider<S, T, R>
    {
        R apply(Twin<State> depts, Twin<State> dests, S symbol1, T symbol2);
    }

    @FunctionalInterface
    interface Finalizer<S>
    {
        void apply(BiMap<Twin<State>, State> stateMapping, Builder<S> builder);
    }
}
