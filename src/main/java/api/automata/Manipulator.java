package api.automata;

public interface Manipulator
{
    <S extends Symbol> Automaton<S> trimUnreachableStates(Automaton<S> target);
}
