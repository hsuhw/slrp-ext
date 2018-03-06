package api.automata;

public interface ImmutableAutomaton<S> extends Automaton<S>
{
    @Override
    TransitionGraph<S> transitionGraph();

    @Override
    default ImmutableAutomaton<S> toImmutable()
    {
        return this;
    }

    interface TransitionGraph<S> extends Automaton.TransitionGraph<S>
    {
    }
}
