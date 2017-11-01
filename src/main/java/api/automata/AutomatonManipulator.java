package api.automata;

import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static api.automata.Automaton.Builder;

public interface AutomatonManipulator
{
    static ImmutableSet<State> selectFromProduct(BiMap<Twin<State>, State> stateMapping, Predicate<State> filter1,
                                                 Predicate<State> filter2, BooleanBooleanPredicate connective)
    {
        final MutableSet<State> result = UnifiedSet.newSet(stateMapping.size()); // upper bound
        stateMapping.forEachKeyValue((statePair, state) -> {
            if (connective.accept(filter1.test(statePair.getOne()), filter2.test(statePair.getTwo()))) {
                result.add(state);
            }
        });

        return result.toImmutable();
    }

    <S> Automaton<S> trimUnreachableStates(Automaton<S> target);

    <S> Automaton<S> trimDeadEndStates(Automaton<S> target);

    <S> Automaton<S> trimDanglingStates(Automaton<S> target);

    <S, R> Automaton<R> project(Automaton<S> target, Alphabet<R> alphabet, Function<S, R> projector);

    <S, T, R> Automaton<R> makeProduct(Automaton<S> one, Automaton<T> two, Alphabet<R> alphabet,
                                       BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer);

    @FunctionalInterface
    interface Finalizer<S>
    {
        void apply(BiMap<Twin<State>, State> stateMapping, Builder<S> builder);
    }
}
