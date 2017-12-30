package cli;

import org.apache.commons.cli.*;

import static api.util.Values.DISPLAY_NEWLINE;

public final class CommandLineInterface
{
    private final String cmdSyntax;
    private final Options options;
    private final HelpFormatter helpFormatter;
    private final CommandLine cmdParsed;

    CommandLineInterface(String[] args) throws ParseException
    {
        cmdSyntax = "slrp-ext [OPTIONS] FILE" + DISPLAY_NEWLINE + DISPLAY_NEWLINE + "options:" + DISPLAY_NEWLINE;
        String helpDesc = "print this message and exit";
        String versionDesc = "print the version information and exit";
        String shapedDesc = "use the shape constraint when searching automata";
        String logLevelDesc = "set the logging level (\"debug\"|\"info\"|\"warn\"|\"error\"|\"fatal\") " //
            + "(default \"warn\")";
        String modeDesc = "set the mode (\"exp\"|\"cav16mono\") (default \"exp\")";

        options = new Options();
        options.addOption("h", "help", false, helpDesc);
        options.addOption("v", "version", false, versionDesc);
        options.addOption("s", "shaped", false, shapedDesc);
        options.addOption(Option.builder("l").longOpt("log-level") //
                                .desc(logLevelDesc).hasArg().argName("LEVEL").build());
        options.addOption(Option.builder("m").longOpt("mode") //
                                .desc(modeDesc).hasArg().argName("MODE").build());

        helpFormatter = new HelpFormatter();
        helpFormatter.setWidth(90);
        helpFormatter.setLeftPadding(2);
        helpFormatter.setLongOptSeparator("=");

        cmdParsed = new DefaultParser().parse(options, args);
    }

    CommandLine invokedCmd()
    {
        return cmdParsed;
    }

    void printHelpMessage()
    {
        helpFormatter.printHelp(cmdSyntax, options);
    }
}
