package core.parser.fsa;

import api.automata.AlphabetTranslator;
import api.automata.fsa.FSA;
import api.parser.ParserWithAlphabet;
import core.automata.StringSymbol;
import generated.AutomatonListLexer;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.BiMaps;
import util.Misc;

import java.io.IOException;
import java.io.InputStream;

public class StringBasicFSAListParser
    implements ParserWithAlphabet<FSA<StringSymbol>, AlphabetTranslator<String, StringSymbol>>
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String EPSILON_SYMBOL = Misc.EPSILON_SYMBOL;

    private final MutableBiMap<String, StringSymbol> symbolTable;

    public StringBasicFSAListParser(MutableBiMap<String, StringSymbol> symbolTable)
    {
        if (!symbolTable.contains(EPSILON_SYMBOL)) {
            symbolTable.put(EPSILON_SYMBOL, new StringSymbol(EPSILON_SYMBOL));
        }
        this.symbolTable = symbolTable;
    }

    @Override
    public AlphabetTranslator<String, StringSymbol> getAlphabetMapping()
    {
        return new core.automata.AlphabetTranslator<>(BiMaps.immutable.ofAll(symbolTable), EPSILON_SYMBOL);
    }

    private ImmutableList<FSA<StringSymbol>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the IntLabelFSA parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        final AutomatonListLexer lexer = new AutomatonListLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final AutomatonListParser parser = new AutomatonListParser(tokens);

        final ParseTree tree = parser.automata();

        final ParseTreeWalker walker = new ParseTreeWalker();
        final StringBasicFSAListener automatonCollector = new StringBasicFSAListener(symbolTable, EPSILON_SYMBOL);
        walker.walk(automatonCollector, tree);

        final long endTime = System.currentTimeMillis();
        LOGGER.info("Source parsed in {}ms.", endTime - startTime);

        return automatonCollector.getAutomata();
    }

    @Override
    public ImmutableList<FSA<StringSymbol>> parse(InputStream stream) throws IOException
    {
        final CharStream charStream = CharStreams.fromStream(stream);
        return parse(charStream);
    }

    @Override
    public ImmutableList<FSA<StringSymbol>> parse(String source)
    {
        final CharStream charStream = CharStreams.fromString(source);
        return parse(charStream);
    }
}
