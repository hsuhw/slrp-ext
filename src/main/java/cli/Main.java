package cli;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import generated.ProblemLexer;
import generated.ProblemParser;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static void setLogLevel(Level level)
    {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(level);
        ctx.updateLoggers();
    }

    public static void main(String[] args) throws Exception
    {
        String filename;

        if (args.length < 1) {
            System.out.println("No input given, doing nothing.");
            return;
        }

        InputStream is = System.in;
        filename = args[0];

        if (filename != null) {
            is = new FileInputStream(filename);
        }

        CharStream input = CharStreams.fromStream(is);
        ProblemLexer lexer = new ProblemLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ProblemParser parser = new ProblemParser(tokens);

        ParseTree tree = parser.problem();

        System.out.println(tree.toStringTree());
    }
}
