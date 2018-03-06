package api.automata;

public interface ImmutableState<S> extends State<S>
{
    @Override
    default ImmutableState<S> toImmutable()
    {
        return this;
    }
}
