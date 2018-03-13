package api.automata;

public interface MutableState<S> extends State<S>
{
    @Override
    default MutableState<S> toMutable()
    {
        return this;
    }

    MutableState<S> setName(String name);

    boolean addTransition(S transLabel, MutableState<S> to);

    MutableState<S> removeTransitionsTo(MutableState<S> state);

    @Override
    String toString();
}
