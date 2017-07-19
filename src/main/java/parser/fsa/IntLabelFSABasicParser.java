package parser.fsa;

import automata.fsa.IntLabelFSA;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import parser.ParserWithAlphabet;
import parser.ParserWithAlphabetAbst;
import parser.generated.AutomatonBasicLexer;
import parser.generated.AutomatonBasicParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class IntLabelFSABasicParser extends ParserWithAlphabetAbst<MutableObjectIntMap<String>>
    implements ParserWithAlphabet<MutableObjectIntMap<String>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    public IntLabelFSABasicParser(MutableObjectIntMap<String> alphabet)
    {
        super(alphabet);
    }

    private List<IntLabelFSA> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the IntLabelFSA parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        final AutomatonBasicLexer lexer = new AutomatonBasicLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final AutomatonBasicParser parser = new AutomatonBasicParser(tokens);

        final ParseTree tree = parser.automata();

        final ParseTreeWalker walker = new ParseTreeWalker();
        final IntLabelFSABasicBuilder automatonCollector = new IntLabelFSABasicBuilder(alphabet);
        walker.walk(automatonCollector, tree);

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Source parsed in {}ms.", endTime - startTime);

        return automatonCollector.getAutomata();
    }

    @Override
    public List<IntLabelFSA> parse(InputStream stream) throws IOException
    {
        final CharStream charStream = CharStreams.fromStream(stream);
        return parse(charStream);
    }

    @Override
    public List<IntLabelFSA> parse(String source)
    {
        final CharStream charStream = CharStreams.fromString(source);
        return parse(charStream);
    }
}
