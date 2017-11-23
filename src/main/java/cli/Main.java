package cli;

import api.parser.Parser;
import api.proof.Problem;
import api.proof.Prover;
import core.parser.StringProblemParser;
import core.proof.CAV16MonoProver;
import core.proof.ExperimentalProver;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

import static api.util.Values.DISPLAY_NEWLINE;

public final class Main
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
        final CommandLineInterface cli = new CommandLineInterface(args);

        // handle general CLI arguments
        if (cli.input().hasOption("help")) {
            cli.printHelpMessage();
            return;
        }
        if (cli.input().hasOption("version")) {
            System.out.println("0.0.0-SNAPSHOT");
            return;
        }
        if (cli.input().hasOption("log-level")) {
            switch (cli.input().getOptionValue("log-level")) {
                case "debug":
                    setLogLevel(Level.DEBUG);
                    break;
                case "info":
                    setLogLevel(Level.INFO);
                    break;
                case "error":
                    setLogLevel(Level.ERROR);
                    break;
                case "fatal":
                    setLogLevel(Level.FATAL);
                    break;
                default:
                    // use the setting in `resources/log4j2.xml`
            }
        }
        if (cli.input().getArgList().isEmpty() || cli.input().getArgList().size() > 1) {
            System.out.println("Input file is not provided correctly." + DISPLAY_NEWLINE);
            cli.printHelpMessage();
            return;
        }

        // parse the input file
        final InputStream input = new FileInputStream(cli.input().getArgList().get(0));
        final Parser<Problem<String>> problemParser = new StringProblemParser();
        final Problem<String> problem = problemParser.parse(input).getOnly();
        LOGGER.debug("Initial config parsed:" + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", //
                     problem.initialConfigs());
        LOGGER.debug("Final config parsed:" + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", //
                     problem.finalConfigs());
        LOGGER.debug("Player 1 parsed:" + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", //
                     problem.scheduler());
        LOGGER.debug("Player 2 parsed:" + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "{}", //
                     problem.process());
        LOGGER.debug("Invariant search bound parsed: {}", problem.invariantSizeBound());
        LOGGER.debug("Order relation search bound parsed: {}", problem.orderSizeBound());

        // process the input problem
        final Prover prover;
        final String mode = cli.input().hasOption("mode") ? cli.input().getOptionValue("mode") : "exp";
        switch (mode) {
            case "cav16mono":
                prover = new CAV16MonoProver<>(problem);
                break;
            default: // should be 'exp'
                prover = new ExperimentalProver<>(problem);
        }
        if (problem.invariant() != null && problem.order() != null) {
            LOGGER.debug("Invoke a verification on input.");
            prover.verify();
        } else {
            LOGGER.debug("Invoke a proof searching on input.");
            prover.prove();
        }
    }
}
