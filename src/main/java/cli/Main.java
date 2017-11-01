package cli;

import api.parser.Parser;
import core.parser.StringProblemParser;
import core.proof.Problem;
import core.proof.Prover;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main
{
    private static void setLogLevel(Level level)
    {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();

        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(level);
        ctx.updateLoggers();
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length < 1) {
            System.out.println("No input given, doing nothing.");
            return;
        }
        if (args.length > 1) {
            setLogLevel(Level.INFO);
        }

        String filename = args[0];
        InputStream is = filename == null ? System.in : new FileInputStream(filename);
        Parser<Problem> parser = new StringProblemParser();
        Problem problem = parser.parse(is).getOnly();

//        System.out.println(problem.initialConfigurations());
//        System.out.println(problem.finalConfigurations());
//        System.out.println(problem.schedulerBehavior());
//        System.out.println(problem.processBehavior());
//        System.out.println(problem.invariantSizeBound());
//        System.out.println(problem.orderRelationSizeBound());

        Prover prover = new Prover(problem);

        if (problem.invariant() != null && problem.orderRelation() != null) {
            prover.verify();
        } else {
            prover.prove();
        }
    }
}
