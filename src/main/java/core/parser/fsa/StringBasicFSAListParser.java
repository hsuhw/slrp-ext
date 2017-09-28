package core.parser.fsa;

import api.automata.fsa.FSA;
import api.parser.Parser;
import generated.AutomatonListLexer;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;

import java.io.IOException;
import java.io.InputStream;

import static api.parser.Parser.SymbolCollectingPolicy.SEPARATE;

public class StringBasicFSAListParser implements Parser<FSA<String>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private ListIterable<FSA<String>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the StringBasicFSA parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        final AutomatonListLexer lexer = new AutomatonListLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final AutomatonListParser parser = new AutomatonListParser(tokens);

        final ParseTree tree = parser.automata();

        final ParseTreeWalker walker = new ParseTreeWalker();
        final StringBasicFSAListener automatonCollector = new StringBasicFSAListener(SEPARATE);
        walker.walk(automatonCollector, tree);

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Source parsed in {}ms.", endTime - startTime);

        return automatonCollector.getAutomata();
    }

    @Override
    public ListIterable<FSA<String>> parse(InputStream stream) throws IOException
    {
        final CharStream charStream = CharStreams.fromStream(stream);
        return parse(charStream);
    }

    @Override
    public ListIterable<FSA<String>> parse(String source)
    {
        final CharStream charStream = CharStreams.fromString(source);
        return parse(charStream);
    }
}
