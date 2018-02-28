package api.automata;

public interface ImmutableState<T> extends State<T>
{
    @Override
    default ImmutableState<T> toImmutable()
    {
        return this;
    }
}
