package core.parser.fsa;

import api.automata.Alphabet;
import api.automata.MutableAutomaton;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.parser.Parser;
import core.parser.AbstractAutomatonListListener;
import generated.AutomatonListBaseListener;
import org.eclipse.collections.api.list.ListIterable;

import static api.parser.Parser.SymbolPolicy;
import static api.util.Constants.DISPLAY_EPSILON_SYMBOL;
import static generated.AutomatonListParser.*;

public class StringFSAListListener extends AutomatonListBaseListener
{
    private static final String EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;

    private final AbstractAutomatonListListener<String> listener;

    public StringFSAListListener(Alphabet<String> alphabet, Parser.SymbolPolicy policy)
    {
        listener = new Listener(alphabet, policy);
    }

    public StringFSAListListener(SymbolPolicy policy)
    {
        listener = new Listener(EPSILON_SYMBOL, policy);
    }

    public StringFSAListListener(Alphabet.Builder<String> alphabetBuilder)
    {
        listener = new Listener(alphabetBuilder);
    }

    public ListIterable<FSA<String>> result()
    {
        @SuppressWarnings("unchecked")
        final ListIterable<FSA<String>> result = (ListIterable) listener.result();
        return result;
    }

    @Override
    public void enterAutomaton(AutomatonContext ctx)
    {
        listener.enterAutomaton(ctx.getStart().getLine(), ctx.getStop().getLine());
    }

    @Override
    public void exitAutomaton(AutomatonContext ctx)
    {
        listener.exitAutomaton();
    }

    @Override
    public void enterStartStates(StartStatesContext ctx)
    {
        listener.enterStartStates(ctx.states().ID());
    }

    @Override
    public void enterTransition(TransitionContext ctx)
    {
        final var symbol = ctx.transitionLabel().emptyLabel() != null // see if it is epsilon symbol
                           ? EPSILON_SYMBOL : ctx.transitionLabel().simpleLabel().getText();

        listener.enterTransition(ctx.ID(), symbol);
    }

    @Override
    public void enterAcceptStates(AcceptStatesContext ctx)
    {
        listener.enterAcceptStates(ctx.states().ID());
    }

    private class Listener extends AbstractAutomatonListListener<String>
    {
        private Listener(Alphabet<String> baseAlphabet, SymbolPolicy policy)
        {
            super(baseAlphabet, policy);
        }

        private Listener(String epsilonSymbol, SymbolPolicy policy)
        {
            super(epsilonSymbol, policy);
        }

        private Listener(Alphabet.Builder<String> alphabetBuilder)
        {
            super(alphabetBuilder);
        }

        @Override
        protected MutableAutomaton<String> newBuilder(Alphabet<String> dummyAlphabet, int stateCapacity)
        {
            return FSAs.create(dummyAlphabet, stateCapacity);
        }
    }
}
