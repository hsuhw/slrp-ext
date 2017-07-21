package parser;

import org.eclipse.collections.api.list.ImmutableList;

import java.io.IOException;
import java.io.InputStream;

public interface Parser
{
    ImmutableList<?> parse(String source);

    ImmutableList<?> parse(InputStream is) throws IOException;
}
