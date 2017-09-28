package api.parser;

import org.eclipse.collections.api.list.ListIterable;

import java.io.IOException;
import java.io.InputStream;

public interface Parser<T>
{
    enum SymbolCollectingPolicy
    {
        PREDEFINED, AGGREGATE, SEPARATE
    }

    ListIterable<T> parse(String source);

    ListIterable<T> parse(InputStream is) throws IOException;
}
