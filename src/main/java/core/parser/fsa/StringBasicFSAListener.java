package core.parser.fsa;

import api.automata.State;
import api.automata.fsa.FSA;
import core.automata.StringSymbol;
import core.automata.fsa.BasicFSABuilder;
import generated.AutomatonListBaseListener;
import generated.AutomatonListParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

public class StringBasicFSAListener extends AutomatonListBaseListener
{
    private final MutableBiMap<String, StringSymbol> symbolTable;
    private final String epsilonSymbol;
    private final MutableList<FSA<StringSymbol>> builtAutomata;
    private MutableMap<String, State> stateTable;
    private BasicFSABuilder<StringSymbol> bookkeeper;

    public StringBasicFSAListener(MutableBiMap<String, StringSymbol> symbolTable, String epsilonSymbol)
    {
        if (!symbolTable.contains(epsilonSymbol)) {
            symbolTable.put(epsilonSymbol, new StringSymbol(epsilonSymbol));
        }
        this.symbolTable = symbolTable;
        this.epsilonSymbol = epsilonSymbol;
        builtAutomata = FastList.newList();
    }

    public ImmutableList<FSA<StringSymbol>> getAutomata()
    {
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
        bookkeeper = new BasicFSABuilder<>(heuristic, symbolTable.get(epsilonSymbol), heuristic);
    }

    @Override
    public void exitAutomaton(AutomatonListParser.AutomatonContext ctx)
    {
        builtAutomata.add(bookkeeper.settleRecords()); // TODO: might need alphabet overriding
        stateTable = null;
        bookkeeper = null;
    }

    private StringSymbol getSymbol(String name)
    {
        return symbolTable.getIfAbsentPut(name, new StringSymbol(name));
    }

    private State getState(String name)
    {
        return stateTable.getIfAbsentPut(name, new core.automata.State(name));
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
                                    ? getSymbol(epsilonSymbol)
                                    : getSymbol(ctx.transitionLabel().monadTransitionLabel().getText());
        bookkeeper.addTransition(dept, dest, symbol);
    }

    @Override
    public void enterAcceptStates(AutomatonListParser.AcceptStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> bookkeeper.addAcceptState(getState(id.getText())));
    }
}
