package cli;

import api.parser.Parser;
import core.Problem;
import core.Prover;
import core.parser.StringProblemParser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

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

        Parser<Problem> parser = new StringProblemParser();
        Problem problem = parser.parse(is).getOnly();

//        System.out.println(problem.getInitialConfigurations());
//        System.out.println(problem.getFinalConfigurations());
//        System.out.println(problem.getSchedulerBehavior());
//        System.out.println(problem.getProcessBehavior());
//        System.out.println(problem.getInvariantConfigSearchSpace());
//        System.out.println(problem.getOrderRelationSearchSpace());

        new Prover(problem).prove();
    }
}
