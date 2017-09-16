package core.parser.fsa;

import api.automata.AlphabetTranslator;
import api.automata.StringSymbol;
import api.automata.fsa.FSA;
import api.parser.ParserWithAlphabet;
import core.automata.AlphabetTranslators;
import core.automata.StringSymbols;
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

import java.io.IOException;
import java.io.InputStream;

import static api.parser.ParserWithAlphabet.SymbolCollectingPolicy.SEPARATE;

public class StringBasicFSAListParser
    implements ParserWithAlphabet<FSA<StringSymbol>, AlphabetTranslator<String, StringSymbol>>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final MutableBiMap<String, StringSymbol> symbolTable;

    public StringBasicFSAListParser(MutableBiMap<String, StringSymbol> symbolTable)
    {
        this.symbolTable = symbolTable;
    }

    @Override
    public AlphabetTranslator<String, StringSymbol> getAlphabetMapping()
    {
        return AlphabetTranslators.createOne(symbolTable, StringSymbols.EPSILON_DISPLAY_VALUE);
    }

    private ImmutableList<FSA<StringSymbol>> parse(CharStream charStream)
    {
        LOGGER.info("Invoke the StringBasicFSA parsing on a given source.");
        final long startTime = System.currentTimeMillis();

        final AutomatonListLexer lexer = new AutomatonListLexer(charStream);
        final TokenStream tokens = new CommonTokenStream(lexer);
        final AutomatonListParser parser = new AutomatonListParser(tokens);

        final ParseTree tree = parser.automata();

        final ParseTreeWalker walker = new ParseTreeWalker();
        final StringBasicFSAListener automatonCollector = new StringBasicFSAListener(symbolTable, SEPARATE);
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
