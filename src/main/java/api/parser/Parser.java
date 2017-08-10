package api.parser;

import org.eclipse.collections.api.list.ImmutableList;

import java.io.IOException;
import java.io.InputStream;

public interface Parser<T>
{
    ImmutableList<T> parse(String source);

    ImmutableList<T> parse(InputStream is) throws IOException;
}
