package core.parser.fsa;

import api.automata.fsa.FSA;
import api.parser.Parser;
import core.parser.AbstractAntlrParser;
import generated.AutomatonListLexer;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;

import static api.parser.Parser.SymbolPolicy.SEPARATE;

public class StringFSAListParser extends AbstractAntlrParser<FSA<String>> implements Parser<FSA<String>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected ListIterable<FSA<String>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the StringBasicFSA parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        // parse the source
        final AutomatonListLexer lexer = new AutomatonListLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final AutomatonListParser parser = new AutomatonListParser(tokens);
        final ParseTree tree = parser.automata();

        // build from the parsed tree
        final ParseTreeWalker walker = new ParseTreeWalker();
        final StringFSAListListener collector = new StringFSAListListener(SEPARATE);
        walker.walk(collector, tree);

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Source parsed in {}ms.", endTime - startTime);

        return collector.getAutomata();
    }
}
