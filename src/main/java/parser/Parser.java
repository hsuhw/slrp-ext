package parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface Parser
{
    List<?> parse(String source);

    List<?> parse(InputStream is) throws IOException;
}
