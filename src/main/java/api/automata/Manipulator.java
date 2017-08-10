package api.automata;

import java.util.function.BiFunction;

public interface Manipulator
{
    <S extends Symbol> Automaton<S> trimUnreachableStates(Automaton<S> target);
}
