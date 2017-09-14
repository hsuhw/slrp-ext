package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;
import api.automata.Symbol;
import core.automata.fsa.BasicFSAStateAttributes;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.BiMaps;

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
            getDecoratee().trimUnreachableStates(target);
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
            getDecoratee().makeProduct(one, two, targetAlphabet, transitionDecider, stateAttributeDecider);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> determinize(FSA<S> target)
    {
        final FSA<S> delegated = determinizeDelegated(target);
        if (delegated == null) {
            getDecoratee().determinize(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> makeCompleteDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> makeComplete(FSA<S> target)
    {
        final FSA<S> delegated = makeCompleteDelegated(target);
        if (delegated == null) {
            getDecoratee().makeComplete(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> minimizeDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> minimize(FSA<S> target)
    {
        final FSA<S> delegated = minimizeDelegated(target);
        if (delegated == null) {
            getDecoratee().minimize(target);
        }
        return delegated;
    }

    <S extends Symbol> FSA<S> makeComplementDelegated(FSA<S> target);

    @Override
    default <S extends Symbol> FSA<S> makeComplement(FSA<S> target)
    {
        final FSA<S> delegated = makeComplementDelegated(target);
        if (delegated == null) {
            getDecoratee().makeComplement(target);
        }
        return delegated;
    }

    default <S extends Symbol> S matchedSymbol(S one, S two)
    {
        return one.equals(two) ? one : null;
    }

    default <S extends Symbol> FSA<S> makeIntersectionDelegated(FSA<S> one, FSA<S> two)
    {
        return makeProductDelegated(one, two, one.getAlphabet(), this::matchedSymbol, (sm, delta) -> {
            final ImmutableList<State> states = sm.keysView().toList().toImmutable();
            final ImmutableBiMap<State, Twin<State>> stateMapping = BiMaps.immutable.ofAll(sm);
            final ImmutableBooleanList startStateTable = makeStartStateIntersection(states, stateMapping, one, two);
            final ImmutableBooleanList acceptStateTable = makeAcceptStateIntersection(states, stateMapping, one, two);
            return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
        });
    }

    @Override
    default <S extends Symbol> FSA<S> makeIntersection(FSA<S> one, FSA<S> two)
    {
        final FSA<S> delegated = makeIntersectionDelegated(one, two);
        if (delegated == null) {
            getDecoratee().makeIntersection(one, two);
        }
        return delegated;
    }

    default <S extends Symbol> FSA<S> makeUnionDelegated(FSA<S> one, FSA<S> two)
    {
        return makeProductDelegated(one, two, one.getAlphabet(), this::matchedSymbol, (sm, delta) -> {
            final ImmutableList<State> states = sm.keysView().toList().toImmutable();
            final ImmutableBiMap<State, Twin<State>> stateMapping = BiMaps.immutable.ofAll(sm);
            final ImmutableBooleanList startStateTable = makeStartStateIntersection(states, stateMapping, one, two);
            final ImmutableBooleanList acceptStateTable = makeAcceptStateUnion(states, stateMapping, one, two);
            return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
        });
    }

    @Override
    default <S extends Symbol> FSA<S> makeUnion(FSA<S> one, FSA<S> two)
    {
        final FSA<S> delegated = makeUnionDelegated(one, two);
        if (delegated == null) {
            getDecoratee().makeUnion(one, two);
        }
        return delegated;
    }
}
