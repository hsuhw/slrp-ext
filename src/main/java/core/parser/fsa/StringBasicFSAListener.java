package core.parser.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.States;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import core.util.Assertions;
import generated.AutomatonListBaseListener;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import static api.parser.Parser.SymbolCollectingPolicy;
import static api.parser.Parser.SymbolCollectingPolicy.*;
import static api.util.Values.DISPLAY_EPSILON_SYMBOL;
import static core.util.Parameters.PARSER_PARSING_TARGET_CAPACITY;

public class StringBasicFSAListener extends AutomatonListBaseListener
{
    private static final String EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;

    private final SymbolCollectingPolicy symbolCollectingPolicy;
    private final MutableList<FSA.Builder<String>> queuedBuilds;
    private final MutableList<FSA<String>> builtAutomata;
    private Alphabet<String> predefinedAlphabet;
    private MutableMap<String, State> stateNameTable;
    private Alphabet.Builder<String> alphabetBuilder;
    private FSA.Builder<String> fsaBuilder;

    public StringBasicFSAListener(Alphabet<String> alphabet, SymbolCollectingPolicy policy)
    {
        Assertions.argumentNotNull(alphabet);

        symbolCollectingPolicy = policy;
        predefinedAlphabet = alphabet;
        if (symbolCollectingPolicy == AGGREGATE) {
            alphabetBuilder = Alphabets.builderBasedOn(alphabet);
        }
        queuedBuilds = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
        builtAutomata = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
    }

    public StringBasicFSAListener(SymbolCollectingPolicy policy)
    {
        if (policy == PREDEFINED) {
            throw new IllegalArgumentException("no predefined alphabet specified");
        }

        symbolCollectingPolicy = policy;
        switch (symbolCollectingPolicy) {
            case AGGREGATE:
                alphabetBuilder = Alphabets.builder(PARSER_PARSING_TARGET_CAPACITY);
                break;
            case SEPARATE:
                predefinedAlphabet = Alphabets.create(Sets.mutable.empty(), EPSILON_SYMBOL);
                break;
            default:
                break; // should not happen
        }
        queuedBuilds = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
        builtAutomata = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
    }

    public StringBasicFSAListener(Alphabet.Builder<String> alphabetBuilder)
    {
        Assertions.argumentNotNull(alphabetBuilder);

        symbolCollectingPolicy = AGGREGATE;
        this.alphabetBuilder = alphabetBuilder;
        queuedBuilds = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
        builtAutomata = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
    }

    public ListIterable<FSA<String>> getAutomata()
    {
        if (symbolCollectingPolicy == AGGREGATE) {
            final Alphabet<String> alphabet = alphabetBuilder.build();
            queuedBuilds.forEach(builder -> builtAutomata.add(builder.build(alphabet)));
        }

        return builtAutomata;
    }

    private int estimateCapacityFactor(ParserRuleContext ctx)
    {
        final int beginLineNum = ctx.getStart().getLine();
        final int endLineNum = ctx.getStop().getLine();
        final int lineSpread = endLineNum - beginLineNum + 1;

        return lineSpread - 4;
    }

    @Override
    public void enterAutomaton(AutomatonListParser.AutomatonContext ctx)
    {
        final int heuristic = estimateCapacityFactor(ctx);
        if (symbolCollectingPolicy == SEPARATE) {
            alphabetBuilder = Alphabets.builderBasedOn(predefinedAlphabet);
        }
        stateNameTable = UnifiedMap.newMap(heuristic);
        fsaBuilder = FSAs.builder(heuristic, EPSILON_SYMBOL, heuristic);
    }

    @Override
    public void exitAutomaton(AutomatonListParser.AutomatonContext ctx)
    {
        switch (symbolCollectingPolicy) {
            case PREDEFINED:
                builtAutomata.add(fsaBuilder.build(predefinedAlphabet));
            case AGGREGATE:
                queuedBuilds.add(fsaBuilder);
                break;
            case SEPARATE:
                builtAutomata.add(fsaBuilder.build(alphabetBuilder.build()));
                alphabetBuilder = null;
                break;
            default:
                break; // should not happen
        }
        stateNameTable = null;
        fsaBuilder = null;
    }

    private State getState(String name)
    {
        return stateNameTable.getIfAbsentPut(name, States.create(name));
    }

    @Override
    public void enterStartStates(AutomatonListParser.StartStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> fsaBuilder.addStartState(getState(id.getText())));
    }

    @Override
    public void enterTransition(AutomatonListParser.TransitionContext ctx)
    {
        final State dept = stateNameTable.get(ctx.ID(0).getText());
        final State dest = stateNameTable.get(ctx.ID(1).getText());
        final String symbol = ctx.transitionLabel().epsilonTransitionLabel() != null
                              ? EPSILON_SYMBOL
                              : ctx.transitionLabel().monadTransitionLabel().getText();
        alphabetBuilder.add(symbol);
        fsaBuilder.addTransition(dept, dest, symbol);
    }

    @Override
    public void enterAcceptStates(AutomatonListParser.AcceptStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> fsaBuilder.addAcceptState(getState(id.getText())));
    }
}
