package core.parser;

import api.parser.Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.eclipse.collections.api.list.ListIterable;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractAntlrParser<S> implements Parser<S>
{
    protected abstract ListIterable<S> parse(CharStream charStream);

    @Override
    public ListIterable<S> parse(String source)
    {
        return parse(CharStreams.fromString(source));
    }

    @Override
    public ListIterable<S> parse(InputStream stream) throws IOException
    {
        return parse(CharStreams.fromStream(stream));
    }
}
