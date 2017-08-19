package api.automata;

import java.util.function.BiFunction;

public interface Manipulator
{
    <S extends Symbol> Automaton<S> trimUnreachableStates(Automaton<S> target);

    <S extends Symbol, T extends Symbol, R extends Symbol> Automaton<S> compose(Automaton<S> first, Automaton<T> after,
                                                                                BiFunction<S, T, R> composer);
}
