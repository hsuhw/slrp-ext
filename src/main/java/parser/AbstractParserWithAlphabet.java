package parser;

import automata.Alphabet;
import org.eclipse.collections.api.list.ImmutableList;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractParserWithAlphabet<T extends Alphabet> implements ParserWithAlphabet<T>
{
    protected final T alphabet;

    public AbstractParserWithAlphabet(T alphabet)
    {
        this.alphabet = alphabet;
    }

    @Override
    public T getAlphabet()
    {
        return alphabet;
    }

    @Override
    public abstract ImmutableList<?> parse(String source);

    @Override
    public abstract ImmutableList<?> parse(InputStream is) throws IOException;
}
