package core.parser.fsa;

import api.automata.Alphabet;
import api.automata.fsa.FSA;
import api.parser.Parser;
import generated.AutomatonListBaseListener;
import org.eclipse.collections.api.list.ListIterable;

import static api.parser.Parser.SymbolPolicy;
import static api.util.Values.DISPLAY_EPSILON_SYMBOL;
import static generated.AutomatonListParser.*;

public class StringFSAListListener extends AutomatonListBaseListener
{
    private static final String EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;

    private final FSAListener<String> impl;

    public StringFSAListListener(Alphabet<String> alphabet, Parser.SymbolPolicy policy)
    {
        impl = new FSAListener<>(alphabet, policy);
    }

    public StringFSAListListener(SymbolPolicy policy)
    {
        impl = new FSAListener<>(EPSILON_SYMBOL, policy);
    }

    public StringFSAListListener(Alphabet.Builder<String> alphabetBuilder)
    {
        impl = new FSAListener<>(alphabetBuilder);
    }

    public ListIterable<FSA<String>> getAutomata()
    {
        return impl.getAutomata();
    }

    @Override
    public void enterAutomaton(AutomatonContext ctx)
    {
        impl.enterAutomaton(ctx.getStart().getLine(), ctx.getStop().getLine());
    }

    @Override
    public void exitAutomaton(AutomatonContext ctx)
    {
        impl.exitAutomaton();
    }

    @Override
    public void enterStartStates(StartStatesContext ctx)
    {
        impl.enterStartStates(ctx.stateList().ID());
    }

    @Override
    public void enterTransition(TransitionContext ctx)
    {
        final String symbol = ctx.transitionLabel().epsilonTransitionLabel() != null
                              ? EPSILON_SYMBOL
                              : ctx.transitionLabel().monadTransitionLabel().getText();

        impl.enterTransition(ctx.ID(), symbol);
    }

    @Override
    public void enterAcceptStates(AcceptStatesContext ctx)
    {
        impl.enterAcceptStates(ctx.stateList().ID());
    }
}
