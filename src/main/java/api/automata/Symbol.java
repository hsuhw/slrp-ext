package api.automata;

public interface Symbol
{
    @Override
    boolean equals(Object other);

    @Override
    int hashCode();

    @Override
    String toString();
}
