package core.parser.fsa;

import api.automata.fsa.FSA;
import api.parser.Parser;
import core.parser.AbstractAntlrParser;
import generated.AutomatonListLexer;
import generated.TransducerListParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Twin;

public class StringRelationFSAListParser extends AbstractAntlrParser<FSA<Twin<String>>>
    implements Parser<FSA<Twin<String>>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected ListIterable<FSA<Twin<String>>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the StringRelationFSA parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        // parse the source
        final AutomatonListLexer lexer = new AutomatonListLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final TransducerListParser parser = new TransducerListParser(tokens);
        final ParseTree tree = parser.transducers();

        // build from the parsed tree
        final ParseTreeWalker walker = new ParseTreeWalker();
        final StringRelationFSAListener automatonCollector = new StringRelationFSAListener();
        walker.walk(automatonCollector, tree);

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Source parsed in {}ms.", endTime - startTime);

        return automatonCollector.getAutomata();
    }
}
