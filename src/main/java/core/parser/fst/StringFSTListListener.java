package core.parser.fst;

import api.automata.Alphabet;
import api.automata.MutableAutomaton;
import api.automata.fst.FST;
import api.automata.fst.FSTs;
import core.parser.AbstractAutomatonListListener;
import generated.TransducerListBaseListener;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.parser.Parser.SymbolPolicy;
import static api.util.Constants.DISPLAY_EPSILON_SYMBOL;
import static generated.TransducerListParser.*;

public class StringFSTListListener extends TransducerListBaseListener
{
    private static final Twin<String> EPSILON_SYMBOL = Tuples.twin(DISPLAY_EPSILON_SYMBOL, DISPLAY_EPSILON_SYMBOL);

    private final AbstractAutomatonListListener<Pair<String, String>> listener;

    public StringFSTListListener(Alphabet<Pair<String, String>> alphabet, SymbolPolicy policy)
    {
        listener = new Listener(alphabet, policy);
    }

    public StringFSTListListener(SymbolPolicy policy)
    {
        listener = new Listener(EPSILON_SYMBOL, policy);
    }

    public StringFSTListListener(Alphabet.Builder<Pair<String, String>> alphabetBuilder)
    {
        listener = new Listener(alphabetBuilder);
    }

    public ListIterable<FST<String, String>> result()
    {
        @SuppressWarnings("unchecked")
        final ListIterable<FST<String, String>> result = (ListIterable) listener.result();
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
    public void enterInOutTransition(InOutTransitionContext ctx)
    {
        final Pair<String, String> symbol;
        final var label = ctx.inOutTransitionLabel();
        symbol = label.emptyLabel() != null
                 ? EPSILON_SYMBOL
                 : Tuples.twin(label.slashedLabel().ID(0).getText(), label.slashedLabel().ID(1).getText());

        listener.enterTransition(ctx.ID(), symbol);
    }

    @Override
    public void enterAcceptStates(AcceptStatesContext ctx)
    {
        listener.enterAcceptStates(ctx.states().ID());
    }

    private class Listener extends AbstractAutomatonListListener<Pair<String, String>>
    {
        private Listener(Alphabet<Pair<String, String>> baseAlphabet, SymbolPolicy policy)
        {
            super(baseAlphabet, policy);
        }

        private Listener(Pair<String, String> epsilonSymbol, SymbolPolicy policy)
        {
            super(epsilonSymbol, policy);
        }

        private Listener(Alphabet.Builder<Pair<String, String>> alphabetBuilder)
        {
            super(alphabetBuilder);
        }

        @Override
        protected MutableAutomaton<Pair<String, String>> newBuilder(Alphabet<Pair<String, String>> dummyAlphabet,
            int stateCapacity)
        {
            return FSTs.create(dummyAlphabet, stateCapacity);
        }
    }
}
