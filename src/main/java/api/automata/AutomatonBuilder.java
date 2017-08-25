package api.automata;

public interface AutomatonBuilder<S extends Symbol>
{
    void addState(State state);

    void addStartState(State state);

    void addAcceptState(State state);

    void addTransition(State dept, State dest, S symbol);

    Automaton<S> build();
}
