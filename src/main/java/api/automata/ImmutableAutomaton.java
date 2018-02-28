package api.automata;

public interface ImmutableAutomaton<S extends ImmutableState<T>, T> extends Automaton<S, T>
{
    @Override
    TransitionGraph<S, T> transitionGraph();

    @Override
    default ImmutableAutomaton<S, T> toImmutable()
    {
        return this;
    }

    interface TransitionGraph<N extends ImmutableState<A>, A> extends Automaton.TransitionGraph<N, A>
    {
    }
}
