package core.parser.fsa;

import api.automata.fsa.FSA;
import api.parser.Parser;
import common.util.Stopwatch;
import core.parser.AbstractAntlrParser;
import generated.AutomatonListLexer;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;

import static api.parser.Parser.SymbolPolicy.SEPARATE;

public class StringFSAListParser extends AbstractAntlrParser<FSA<String>> implements Parser<FSA<String>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private long profilingStartTime;

    @Override
    protected ListIterable<FSA<String>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the Automata parsing at thread time {}ms.", //
                    () -> (profilingStartTime = Stopwatch.currentThreadCpuTimeInMs()));

        final TokenStream tokens = new CommonTokenStream(new AutomatonListLexer(charStream));
        final var parseTreeHandler = new AutomatonListParser(tokens);
        final var collector = new StringFSAListListener(SEPARATE);
        final var parseTreeWalker = new ParseTreeWalker();
        parseTreeWalker.walk(collector, parseTreeHandler.automatonList());

        LOGGER.info("Automata parsed in {}ms.", //
                    () -> Stopwatch.currentThreadCpuTimeInMs() - profilingStartTime);

        return collector.result();
    }
}
