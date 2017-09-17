package api.automata;

import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import util.Predicates;

import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface AutomatonManipulator
{
    default ImmutableBooleanList makeStatePredicateTableWith(ImmutableList<State> targetStates,
                                                             ImmutableBiMap<State, Twin<State>> stateMapping,
                                                             Predicate<State> checker1, Predicate<State> checker2,
                                                             BooleanBooleanPredicate concluder)
    {
        final int targetSize = targetStates.size();
        MutableBooleanList intersection = BooleanLists.mutable.of(new boolean[targetSize]);
        for (int i = 0; i < targetSize; i++) {
            final Twin<State> origin = stateMapping.get(targetStates.get(i));
            intersection.set(i, concluder.accept(checker1.test(origin.getOne()), checker2.test(origin.getTwo())));
        }
        return intersection.toImmutable();
    }

    default <S extends Symbol, T extends Symbol> ImmutableBooleanList makeStartStateIntersection(
        ImmutableList<State> targetStates, ImmutableBiMap<State, Twin<State>> stateMapping, Automaton<S> one,
        Automaton<T> two)
    {
        return makeStatePredicateTableWith(targetStates, stateMapping, one::isStartState, two::isStartState,
                                           Predicates.AND);
    }

    default ImmutableBooleanList makeAcceptStateComplement(ImmutableBooleanList target)
    {
        final MutableBooleanList complement = target.toList();
        for (int i = 0; i < complement.size(); i++) {
            complement.set(i, !complement.get(i));
        }
        return complement.toImmutable();
    }

    default <S extends Symbol, T extends Symbol> ImmutableBooleanList makeAcceptStateIntersection(
        ImmutableList<State> targetStates, ImmutableBiMap<State, Twin<State>> stateMapping, Automaton<S> one,
        Automaton<T> two)
    {
        return makeStatePredicateTableWith(targetStates, stateMapping, one::isAcceptState, two::isAcceptState,
                                           Predicates.AND);
    }

    default <S extends Symbol, T extends Symbol> ImmutableBooleanList makeAcceptStateUnion(
        ImmutableList<State> targetStates, ImmutableBiMap<State, Twin<State>> stateMapping, Automaton<S> one,
        Automaton<T> two)
    {
        return makeStatePredicateTableWith(targetStates, stateMapping, one::isAcceptState, two::isAcceptState,
                                           Predicates.OR);
    }

    <S extends Symbol> Automaton<S> trimUnreachableStates(Automaton<S> target);

    <S extends Symbol> Automaton<S> trimDeadEndStates(Automaton<S> target);

    <S extends Symbol, T extends Symbol, R extends Symbol> Automaton<R> makeProduct(Automaton<S> one, Automaton<T> two,
                                                                                    Alphabet<R> targetAlphabet,
                                                                                    BiFunction<S, T, R> transitionDecider,
                                                                                    StateAttributeDecider<R> stateAttributeDecider);

    interface StateAttributes
    {
        ImmutableList<State> getDefinitionOfStates();

        ImmutableBooleanList getStartStateTable();

        ImmutableBooleanList getAcceptStateTable();
    }

    @FunctionalInterface
    interface StateAttributeDecider<S extends Symbol>
    {
        StateAttributes decide(ImmutableBiMap<State, Twin<State>> stateMapping, TransitionFunction<S> newDelta);
    }
}
