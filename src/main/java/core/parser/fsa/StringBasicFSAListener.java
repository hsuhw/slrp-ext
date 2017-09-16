package core.parser.fsa;

import api.automata.Alphabet;
import api.automata.State;
import api.automata.StringSymbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSABuilder;
import api.parser.ParserWithAlphabet;
import core.automata.Alphabets;
import core.automata.States;
import core.automata.StringSymbols;
import core.automata.fsa.BasicFSABuilder;
import core.automata.fsa.FSABuilders;
import generated.AutomatonListBaseListener;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import static api.parser.ParserWithAlphabet.SymbolCollectingPolicy.AGGREGATE;
import static api.parser.ParserWithAlphabet.SymbolCollectingPolicy.PREDEFINED;

public class StringBasicFSAListener extends AutomatonListBaseListener
{
    private final MutableBiMap<String, StringSymbol> symbolTable;
    private final ParserWithAlphabet.SymbolCollectingPolicy symbolCollectingPolicy;
    private final MutableList<FSABuilder<StringSymbol>> postponeBuilds;
    private final MutableList<FSA<StringSymbol>> builtAutomata;
    private Alphabet<StringSymbol> currentAlphabet;
    private MutableMap<String, State> stateTable;
    private BasicFSABuilder<StringSymbol> bookkeeper;

    public StringBasicFSAListener(MutableBiMap<String, StringSymbol> symbolTable,
                                  ParserWithAlphabet.SymbolCollectingPolicy symbolCollectingPolicy)
    {
        if (!symbolTable.containsValue(StringSymbols.EPSILON_DISPLAY_VALUE)) {
            symbolTable.put(StringSymbols.EPSILON_DISPLAY_VALUE, StringSymbols.EPSILON);
        }
        this.symbolTable = symbolTable;
        this.symbolCollectingPolicy = symbolCollectingPolicy;
        currentAlphabet = symbolCollectingPolicy == PREDEFINED ? getCurrentAlphabet() : null;
        postponeBuilds = FastList.newList();
        builtAutomata = FastList.newList();
    }

    public Alphabet<StringSymbol> getCurrentAlphabet()
    {
        return Alphabets.createOne(symbolTable.valuesView().toSet(), StringSymbols.EPSILON);
    }

    public ImmutableList<FSA<StringSymbol>> getAutomata()
    {
        if (symbolCollectingPolicy == AGGREGATE) {
            currentAlphabet = getCurrentAlphabet();
            postponeBuilds.forEach(builder -> builtAutomata.add(builder.build(currentAlphabet)));
        }
        return builtAutomata.toImmutable();
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
        stateTable = UnifiedMap.newMap(heuristic);
        bookkeeper = FSABuilders.createBasic(heuristic, StringSymbols.EPSILON, heuristic);
    }

    @Override
    public void exitAutomaton(AutomatonListParser.AutomatonContext ctx)
    {
        switch (symbolCollectingPolicy) {
            case PREDEFINED:
                builtAutomata.add(bookkeeper.build(currentAlphabet));
            case AGGREGATE:
                postponeBuilds.add(bookkeeper);
                break;
            case SEPARATE:
                builtAutomata.add(bookkeeper.build(getCurrentAlphabet()));
                break;
            default:
                break;
        }
        stateTable = null;
        bookkeeper = null;
    }

    private StringSymbol getSymbol(String name)
    {
        return symbolTable.getIfAbsentPut(name, StringSymbols.createOne(name));
    }

    private State getState(String name)
    {
        return stateTable.getIfAbsentPut(name, States.createOne(name));
    }

    @Override
    public void enterStartStates(AutomatonListParser.StartStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> bookkeeper.addStartState(getState(id.getText())));
    }

    @Override
    public void enterTransition(AutomatonListParser.TransitionContext ctx)
    {
        final State dept = stateTable.get(ctx.ID(0).getText());
        final State dest = stateTable.get(ctx.ID(1).getText());
        final StringSymbol symbol = ctx.transitionLabel().epsilonTransitionLabel() != null
                                    ? StringSymbols.EPSILON
                                    : getSymbol(ctx.transitionLabel().monadTransitionLabel().getText());
        bookkeeper.addTransition(dept, dest, symbol);
    }

    @Override
    public void enterAcceptStates(AutomatonListParser.AcceptStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> bookkeeper.addAcceptState(getState(id.getText())));
    }
}
