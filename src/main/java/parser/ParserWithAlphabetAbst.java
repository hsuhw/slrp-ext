package parser;

public abstract class ParserWithAlphabetAbst<T> implements ParserWithAlphabet<T>
{
    protected final T alphabet;

    public ParserWithAlphabetAbst(T alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public T getAlphabet()
    {
        return alphabet;
    }
}
