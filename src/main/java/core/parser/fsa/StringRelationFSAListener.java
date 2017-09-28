package core.parser.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.States;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import core.util.Assertions;
import generated.ProblemBaseListener;
import generated.ProblemParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.util.Values.DISPLAY_EPSILON_SYMBOL;
import static core.util.Parameters.PARSER_PARSING_TARGET_CAPACITY;

public class StringRelationFSAListener extends ProblemBaseListener
{
    private static final String ORIGIN_EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;
    private static final Twin<String> EPSILON_SYMBOL = Tuples.twin(DISPLAY_EPSILON_SYMBOL, DISPLAY_EPSILON_SYMBOL);

    private final MutableList<FSA<Twin<String>>> builtAutomata;
    private MutableMap<String, State> stateNameTable;
    private Alphabet.Builder<String> alphabetRecorder;
    private Alphabet.Builder<Twin<String>> alphabetBuilder;
    private FSA.Builder<Twin<String>> fsaBuilder;

    public StringRelationFSAListener(Alphabet.Builder<String> alphabetBuilder)
    {
        Assertions.argumentNotNull(alphabetBuilder);

        alphabetRecorder = alphabetBuilder;
        builtAutomata = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
    }

    public StringRelationFSAListener()
    {
        alphabetRecorder = Alphabets.builder(PARSER_PARSING_TARGET_CAPACITY, ORIGIN_EPSILON_SYMBOL);
        builtAutomata = FastList.newList(PARSER_PARSING_TARGET_CAPACITY);
    }

    public ListIterable<FSA<Twin<String>>> getAutomata()
    {
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
    public void enterTransducer(ProblemParser.TransducerContext ctx)
    {
        final int heuristic = estimateCapacityFactor(ctx);
        stateNameTable = UnifiedMap.newMap(heuristic);
        alphabetBuilder = Alphabets.builder(heuristic, EPSILON_SYMBOL);
        fsaBuilder = FSAs.builder(heuristic, EPSILON_SYMBOL, heuristic);

        ctx.startStates().enterRule(this);
        ctx.transducerTransitions().transducerTransition().forEach(trans -> trans.enterRule(this));
        ctx.acceptStates().enterRule(this);
        ctx.exitRule(this);
    }

    @Override
    public void exitTransducer(ProblemParser.TransducerContext ctx)
    {
        builtAutomata.add(fsaBuilder.build(alphabetBuilder.build()));
        stateNameTable = null;
        alphabetBuilder = null;
        fsaBuilder = null;
    }

    private State getState(String name)
    {
        return stateNameTable.computeIfAbsent(name, __ -> States.create(name));
    }

    @Override
    public void enterStartStates(ProblemParser.StartStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> fsaBuilder.addStartState(getState(id.getText())));
    }

    @Override
    public void enterTransducerTransition(ProblemParser.TransducerTransitionContext ctx)
    {
        final State dept = getState(ctx.ID(0).getText());
        final State dest = getState(ctx.ID(1).getText());
        final Twin<String> symbol;
        if (ctx.transducerTransitionLabel().epsilonTransitionLabel() != null) {
            symbol = EPSILON_SYMBOL;
            alphabetBuilder.add(EPSILON_SYMBOL);
        } else {
            final String input = ctx.transducerTransitionLabel().monadIOTransitionLabel().ID(0).getText();
            final String output = ctx.transducerTransitionLabel().monadIOTransitionLabel().ID(1).getText();
            symbol = Tuples.twin(input, output);
            alphabetBuilder.add(symbol);
            alphabetRecorder.add(input);
            alphabetRecorder.add(output);
        }
        fsaBuilder.addTransition(dept, dest, symbol);
    }

    @Override
    public void enterAcceptStates(ProblemParser.AcceptStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> fsaBuilder.addAcceptState(getState(id.getText())));
    }
}
