package api.automata;

public interface State
{
    String name();

    @Override
    String toString();

    interface Provider
    {
        String GENERATED_PREFIX = "$";

        State create(String name);

        State generate();
    }
}
