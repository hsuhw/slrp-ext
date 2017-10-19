package core.parser;

import api.parser.Parser;
import core.Problem;
import generated.ProblemLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;

public class StringProblemParser extends AbstractAntlrParser<Problem> implements Parser<Problem>
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected ListIterable<Problem> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the Problem instance parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        // parse the source
        final ProblemLexer lexer = new ProblemLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final generated.ProblemParser parser = new generated.ProblemParser(tokens);
        final ParseTree tree = parser.problem();

        // build from the parsed tree
        final ParseTreeWalker walker = new ParseTreeWalker();
        final StringProblemListener collector = new StringProblemListener();
        walker.walk(collector, tree);

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Source parsed in {}ms.", endTime - startTime);

        return collector.getProblem();
    }
}
