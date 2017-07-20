package parser;

import automata.Alphabet;

public abstract class ParserWithAlphabetAbst<T extends Alphabet> implements ParserWithAlphabet<T>
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
