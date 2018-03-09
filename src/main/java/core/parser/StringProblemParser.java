package core.parser;

import api.parser.Parser;
import api.proof.Problem;
import common.util.Stopwatch;
import generated.ProblemLexer;
import generated.ProblemParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.ListIterable;

public class StringProblemParser extends AbstractAntlrParser<Problem<String>> implements Parser<Problem<String>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private long profilingStartTime;

    @Override
    protected ListIterable<Problem<String>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the Problem parsing at thread time {}ms.", //
                    () -> (profilingStartTime = Stopwatch.currentThreadCpuTimeInMs()));

        final ProblemParser parseTreeHandler = new ProblemParser(new CommonTokenStream(new ProblemLexer(charStream)));
        final ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
        final StringProblemListener collector = new StringProblemListener();
        parseTreeWalker.walk(collector, parseTreeHandler.problem());

        LOGGER.info("Problem parsed in {}ms.", //
                    () -> Stopwatch.currentThreadCpuTimeInMs() - profilingStartTime);

        return collector.result();
    }
}
