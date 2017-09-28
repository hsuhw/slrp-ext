package core.parser;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import core.Problem;
import core.parser.fsa.StringBasicFSAListener;
import core.parser.fsa.StringRelationFSAListener;
import generated.ProblemParser;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import static api.util.Values.DISPLAY_EPSILON_SYMBOL;
import static core.util.Parameters.PARSER_PARSING_TARGET_CAPACITY;

public class ProblemListener extends generated.ProblemBaseListener
{
    private static final String EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;

    private final StringBasicFSAListener initialConfigListener;
    private final StringBasicFSAListener finalConfigListener;
    private final StringRelationFSAListener schedulerListener;
    private final StringRelationFSAListener processListener;
    private IntIntPair invariantBound;
    private IntIntPair relationBound;

    public ProblemListener()
    {
        final Alphabet.Builder<String> alphabetRecorder = Alphabets
            .builder(PARSER_PARSING_TARGET_CAPACITY, EPSILON_SYMBOL);
        initialConfigListener = new StringBasicFSAListener(alphabetRecorder);
        finalConfigListener = new StringBasicFSAListener(alphabetRecorder);
        schedulerListener = new StringRelationFSAListener(alphabetRecorder);
        processListener = new StringRelationFSAListener(alphabetRecorder);
    }

    public ListIterable<Problem> getProblem()
    {
        final FSA<String> initialCfg = initialConfigListener.getAutomata().getOnly();
        final FSA<String> finalCfg = finalConfigListener.getAutomata().getOnly();
        final FSA<Twin<String>> p1 = schedulerListener.getAutomata().getOnly();
        final FSA<Twin<String>> p2 = processListener.getAutomata().getOnly();
        final Problem result = new Problem(initialCfg, finalCfg, p1, p2, invariantBound, relationBound);

        return Lists.immutable.of(result);
    }

    @Override
    public void enterInitialStatesRepr(ProblemParser.InitialStatesReprContext ctx)
    {
        ctx.automaton().enterRule(initialConfigListener);
    }

    @Override
    public void enterFinalStatesRepr(ProblemParser.FinalStatesReprContext ctx)
    {
        ctx.automaton().enterRule(finalConfigListener);
    }

    @Override
    public void enterSchedulerRepr(ProblemParser.SchedulerReprContext ctx)
    {
        ctx.transducer().enterRule(schedulerListener);
    }

    @Override
    public void enterProcessRepr(ProblemParser.ProcessReprContext ctx)
    {
        ctx.transducer().enterRule(processListener);
    }

    @Override
    public void enterInvariantSearchSpace(ProblemParser.InvariantSearchSpaceContext ctx)
    {
        final int from = Integer.parseInt(ctx.integerRange().INTEGER(0).getText());
        final int to = Integer.parseInt(ctx.integerRange().INTEGER(1).getText());
        invariantBound = PrimitiveTuples.pair(from, to);
    }

    @Override
    public void enterRelationSearchSpace(ProblemParser.RelationSearchSpaceContext ctx)
    {
        final int from = Integer.parseInt(ctx.integerRange().INTEGER(0).getText());
        final int to = Integer.parseInt(ctx.integerRange().INTEGER(1).getText());
        relationBound = PrimitiveTuples.pair(from, to);
    }
}
