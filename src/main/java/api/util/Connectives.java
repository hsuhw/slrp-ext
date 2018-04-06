package api.util;

import api.automata.Automaton;
import api.automata.MutableState;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.function.Function;

import static api.automata.Automaton.Finalizer;
import static api.automata.Automaton.StepMaker;

public interface Connectives
{
    BooleanBooleanPredicate AND = (a, b) -> a && b;
    BooleanBooleanPredicate OR = (a, b) -> a || b;

    interface Labels
    {
        static <S, T> Function<Pair<S, T>, Pair<T, S>> flipped()
        {
            return label -> Tuples.pair(label.getTwo(), label.getOne());
        }

        static <S> StepMaker<S, S, S> matched()
        {
            return (statePair, s1, s2) -> s1.equals(s2) ? s1 : null;
        }

        static <S, T, U extends Pair<S, T>> StepMaker<U, S, U> inputMatched()
        {
            return (statePair, inOut, symbol) -> symbol.equals(inOut.getOne()) ? inOut : null;
        }

        static <S, T, U extends Pair<S, T>> StepMaker<U, T, U> outputMatched()
        {
            return (statePair, inOut, symbol) -> symbol.equals(inOut.getTwo()) ? inOut : null;
        }

        static <S, T, U extends Pair<S, T>> StepMaker<U, S, T> transduced()
        {
            return (statePair, inOut, symbol) -> symbol.equals(inOut.getOne()) ? inOut.getTwo() : null;
        }

        static <S, T, R, U extends Pair<S, T>, V extends Pair<T, R>> StepMaker<U, V, Pair<S, R>> composed()
        {
            return (statePair, s1, s2) -> s1.getTwo().equals(s2.getOne())
                                          ? Tuples.pair(s1.getOne(), s2.getTwo())
                                          : null;
        }
    }

    interface AcceptStates
    {
        static <S, T, R> Finalizer<S, T, R> select(Automaton<S> one, Automaton<T> two, BooleanBooleanPredicate matcher)
        {
            return (stateMapping, builder) -> {
                final var mapping = stateMapping.inverse();
                builder.states().forEach(state -> {
                    final var state1 = mapping.get(state).getOne();
                    final var state2 = mapping.get(state).getTwo();
                    if (matcher.accept(one.isAcceptState(state1), two.isAcceptState(state2))) {
                        builder.setAsAccept((MutableState<R>) state);
                    }
                });
            };
        }
    }
}
