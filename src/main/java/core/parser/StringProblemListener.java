package core.parser;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.MutableAutomaton;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fst.FST;
import api.automata.fst.FSTs;
import api.proof.Problem;
import core.proof.BasicProblem;
import generated.ProblemBaseListener;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import static api.util.Constants.DISPLAY_EPSILON_SYMBOL;
import static generated.ProblemParser.*;

public class StringProblemListener extends ProblemBaseListener
{
    private static final String EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;
    private static final Twin<String> TWIN_EPSILON_SYMBOL = Tuples.twin(EPSILON_SYMBOL, EPSILON_SYMBOL);

    private FSAListener fsaListener;
    private FSTListener fstListener;
    private int commonCapacity;
    private IntIntPair invSize;
    private IntIntPair ordSize;
    private boolean invEnclosesAll;

    public StringProblemListener()
    {
    }

    public ListIterable<Problem<String>> result()
    {
        final var init = fsaListener.result().get(0);
        final var fin = fsaListener.result().get(1);
        final var inv = fsaListener.result().size() == 2 ? null : fsaListener.result().getLast();
        final var sched = fstListener.result().get(0);
        final var proc = fstListener.result().get(1);
        final var ord = fstListener.result().size() == 2 ? null : fstListener.result().getLast();
        final Problem<String> problem = new BasicProblem<>(init, fin, sched, proc, inv, ord, invSize, ordSize,
                                                           invEnclosesAll);

        return Lists.immutable.of(problem);
    }

    @Override
    public void enterProblem(ProblemContext ctx)
    {
        commonCapacity = (ctx.getStop().getLine() - ctx.getStart().getLine()) / 4; // loose upper bound
        final var alphabetRecorder = Alphabets.builder(commonCapacity, EPSILON_SYMBOL);
        fsaListener = new FSAListener(alphabetRecorder);
        fstListener = new FSTListener(alphabetRecorder);
    }

    @Override
    public void enterInitialConfigs(InitialConfigsContext ctx)
    {
        ctx.automaton().enterRule(fsaListener);
    }

    @Override
    public void enterFinalConfigs(FinalConfigsContext ctx)
    {
        ctx.automaton().enterRule(fsaListener);
    }

    @Override
    public void enterScheduler(SchedulerContext ctx)
    {
        ctx.transducer().enterRule(fstListener);
    }

    @Override
    public void enterProcess(ProcessContext ctx)
    {
        ctx.transducer().enterRule(fstListener);
    }

    @Override
    public void enterInvariant(InvariantContext ctx)
    {
        ctx.automaton().enterRule(fsaListener);
    }

    @Override
    public void enterOrder(OrderContext ctx)
    {
        ctx.transducer().enterRule(fstListener);
    }

    private static IntIntPair sortedIntIntPair(int one, int two)
    {
        return one > two ? PrimitiveTuples.pair(two, one) : PrimitiveTuples.pair(one, two);
    }

    @Override
    public void enterInvariantSizeBound(InvariantSizeBoundContext ctx)
    {
        final var from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 0);
        final var to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 0);
        invSize = sortedIntIntPair(from, to);
    }

    @Override
    public void enterOrderSizeBound(OrderSizeBoundContext ctx)
    {
        final var from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 0);
        final var to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 0);
        ordSize = sortedIntIntPair(from, to);
    }

    @Override
    public void enterClosedUnderTransFlag(ClosedUnderTransFlagContext ctx)
    {
        invEnclosesAll = true;
    }

    @Override
    public void enterClosedUnderTrans(ClosedUnderTransContext ctx)
    {
        invEnclosesAll = true;
    }

    private class FSAListener extends ProblemBaseListener
    {
        private final AbstractAutomatonListListener<String> listener;

        private FSAListener(Alphabet.Builder<String> alphabetRecorder)
        {
            listener = new AbstractAutomatonListListener<>(alphabetRecorder)
            {
                @Override
                protected MutableAutomaton<String> newBuilder(Alphabet<String> dummyAlphabet, int stateCapacity)
                {
                    return FSAs.create(dummyAlphabet, stateCapacity);
                }
            };
        }

        private ListIterable<FSA<String>> result()
        {
            @SuppressWarnings("unchecked")
            final ListIterable<FSA<String>> result = (ListIterable) listener.result();
            return result;
        }

        @Override
        public void enterAutomaton(AutomatonContext ctx)
        {
            listener.enterAutomaton(ctx.getStart().getLine(), ctx.getStop().getLine());
            ctx.startStates().enterRule(this);
            ctx.transitions().transition().forEach(transCtx -> transCtx.enterRule(this));
            ctx.acceptStates().enterRule(this);
            ctx.exitRule(this);
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
    }

    private class FSTListener extends ProblemBaseListener
    {
        private final AbstractAutomatonListListener<Pair<String, String>> listener;
        private final Alphabet.Builder<String> alphabetRecorder;

        private FSTListener(Alphabet.Builder<String> alphabetRecorder)
        {
            listener = new AbstractAutomatonListListener<>(Alphabets.builder(commonCapacity, TWIN_EPSILON_SYMBOL))
            {
                @Override
                protected MutableAutomaton<Pair<String, String>> newBuilder(
                    Alphabet<Pair<String, String>> dummyAlphabet, int stateCapacity)
                {
                    return FSTs.create(dummyAlphabet, stateCapacity);
                }
            };
            this.alphabetRecorder = alphabetRecorder;
        }

        private ListIterable<FST<String, String>> result()
        {
            @SuppressWarnings("unchecked")
            final ListIterable<FST<String, String>> result = (ListIterable) listener.result();
            return result;
        }

        @Override
        public void enterTransducer(TransducerContext ctx)
        {
            listener.enterAutomaton(ctx.getStart().getLine(), ctx.getStop().getLine());
            ctx.startStates().enterRule(this);
            ctx.inOutTransitions().inOutTransition().forEach(transCtx -> transCtx.enterRule(this));
            ctx.acceptStates().enterRule(this);
            ctx.exitRule(this);
        }

        @Override
        public void exitTransducer(TransducerContext ctx)
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
                     ? TWIN_EPSILON_SYMBOL
                     : Tuples.twin(label.slashedLabel().ID(0).getText(), label.slashedLabel().ID(1).getText());

            alphabetRecorder.add(symbol.getOne()).add(symbol.getTwo());
            listener.enterTransition(ctx.ID(), symbol);
        }

        @Override
        public void enterAcceptStates(AcceptStatesContext ctx)
        {
            listener.enterAcceptStates(ctx.states().ID());
        }
    }
}
