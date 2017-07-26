package parser;

import automata.Alphabet;
import org.eclipse.collections.api.list.ImmutableList;

import java.io.IOException;
import java.io.InputStream;

public interface ParserWithAlphabet<T extends Alphabet> extends Parser
{
    T getAlphabet();

    @Override
    ImmutableList<?> parse(String source);

    @Override
    ImmutableList<?> parse(InputStream is) throws IOException;
}
