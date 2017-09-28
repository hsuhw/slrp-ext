package api.automata;

import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import static api.automata.Automaton.Builder;

public interface AutomatonManipulator
{
    static SetIterable<State> selectStatesFromProduct(BiMap<Twin<State>, State> stateMapping,
                                                      Predicate<State> filterOne, Predicate<State> filterTwo,
                                                      BooleanBooleanPredicate connective)
    {
        final MutableSet<State> result = UnifiedSet.newSet(stateMapping.size()); // upper bound
        stateMapping.forEachKeyValue((statePair, state) -> {
            if (connective.accept(filterOne.test(statePair.getOne()), filterTwo.test(statePair.getTwo()))) {
                result.add(state);
            }
        });

        return result;
    }

    <S> Automaton<S> trimUnreachableStates(Automaton<S> target);

    <S> Automaton<S> trimDeadEndStates(Automaton<S> target);

    <S, T, R> Automaton<R> makeProduct(Automaton<S> one, Automaton<T> two, Alphabet<R> targetAlphabet,
                                       BiFunction<S, T, R> transitionDecider, Finalizer<R> finalizer);

    @FunctionalInterface
    interface Finalizer<S>
    {
        void apply(BiMap<Twin<State>, State> stateMapping, Builder<S> builder);
    }
}
