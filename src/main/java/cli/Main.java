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

import java.io.FileInputStream;
import java.io.InputStream;

import static common.util.Constants.DISPLAY_NEWLINE;

public final class Main
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static void setLogLevel(Level level)
    {
        final var ctx = (LoggerContext) LogManager.getContext(false);
        final var config = ctx.getConfiguration();
        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(level);
        ctx.updateLoggers();
    }

    public static void main(String[] args) throws Exception
    {
        final var cli = new CommandLineInterface(args);

        // handle general CLI arguments
        if (cli.invokedCmd().hasOption("help")) {
            cli.printHelpMessage();
            return;
        }
        if (cli.invokedCmd().hasOption("version")) {
            System.out.println("0.0.0-SNAPSHOT");
            return;
        }
        if (cli.invokedCmd().hasOption("log-level")) {
            switch (cli.invokedCmd().getOptionValue("log-level")) {
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
        if (cli.invokedCmd().getArgList().isEmpty() || cli.invokedCmd().getArgList().size() > 1) {
            System.out.println("Input file is not provided correctly." + DISPLAY_NEWLINE);
            cli.printHelpMessage();
            return;
        }
        if (cli.invokedCmd().hasOption("wait-for-profiler")) {
            System.out.print("Press the enter key to continue ...");
            System.out.println(System.in.read() == 10 ? "" : "");
        }

        // parse the input file
        final InputStream input = new FileInputStream(cli.invokedCmd().getArgList().get(0));
        final Parser<Problem<String>> problemParser = new StringProblemParser();
        final var problem = problemParser.parse(input).getOnly();
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
        final var mode = cli.invokedCmd().hasOption("mode") ? cli.invokedCmd().getOptionValue("mode") : "exp";
        final var shapeInvariant = cli.invokedCmd().hasOption("shape-invariant");
        final var shapeOrder = cli.invokedCmd().hasOption("shape-order");
        final var loosenInvariant = cli.invokedCmd().hasOption("loose-invariant");
        switch (mode) {
            case "cav16mono":
                prover = new CAV16MonoProver<>(problem, shapeInvariant, shapeOrder, loosenInvariant);
                break;
            default: // should be 'exp'
                prover = new ExperimentalProver<>(problem, shapeInvariant, shapeOrder, loosenInvariant);
        }
        if (problem.invariant() != null && problem.order() != null) {
            LOGGER.info("Invoke a verification on input.");
            prover.verify();
        } else {
            LOGGER.info("Invoke a proof searching on input.");
            prover.prove();
        }
    }
}
