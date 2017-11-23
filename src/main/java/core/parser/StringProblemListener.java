package core.parser;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import api.proof.Problem;
import core.parser.fsa.FSAListener;
import core.proof.BasicProblem;
import generated.ProblemBaseListener;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import static api.util.Values.DISPLAY_EPSILON_SYMBOL;
import static generated.ProblemParser.*;

public class StringProblemListener extends ProblemBaseListener
{
    private static final String EPSILON_SYMBOL = DISPLAY_EPSILON_SYMBOL;
    private static final Twin<String> TRANSDUCER_EPSILON_SYMBOL = Tuples.twin(EPSILON_SYMBOL, EPSILON_SYMBOL);

    private int commonCapacity;
    private Alphabet.Builder<String> alphabetRecorder;
    private FSAPartListener initialConfigsListener;
    private FSAPartListener finalConfigsListener;
    private TransducerPartListener schedulerListener;
    private TransducerPartListener processListener;
    private FSAPartListener invariantListener;
    private TransducerPartListener orderListener;
    private IntIntPair invSize;
    private IntIntPair ordSize;
    private boolean invEnclosesAll;

    public ListIterable<Problem<String>> getProblem()
    {
        final FSA<String> init = initialConfigsListener.getAutomata().getOnly();
        final FSA<String> fin = finalConfigsListener.getAutomata().getOnly();
        final ListIterable<FSA<String>> invariantParsed = invariantListener.getAutomata();
        final FSA<String> inv = invariantParsed.notEmpty() ? invariantParsed.getOnly() : null;
        final Alphabet<Twin<String>> relationAlphabet = Alphabets.product(init.alphabet());
        final FSA<Twin<String>> sched = schedulerListener.getAutomataWith(relationAlphabet).getOnly();
        final FSA<Twin<String>> proc = processListener.getAutomataWith(relationAlphabet).getOnly();
        final ListIterable<FSA<Twin<String>>> orderParsed = orderListener.getAutomataWith(relationAlphabet);
        final FSA<Twin<String>> ord = orderParsed.notEmpty() ? orderParsed.getOnly() : null;
        final Problem<String> problem = new BasicProblem<>(init, fin, sched, proc, inv, ord, invSize, ordSize,
                                                           invEnclosesAll);

        return Lists.immutable.of(problem);
    }

    @Override
    public void enterProblem(ProblemContext ctx)
    {
        commonCapacity = (ctx.getStop().getLine() - ctx.getStart().getLine()) / 4; // loose upper bound
        alphabetRecorder = Alphabets.builder(commonCapacity, EPSILON_SYMBOL);
        initialConfigsListener = new FSAPartListener();
        finalConfigsListener = new FSAPartListener();
        invariantListener = new FSAPartListener();
        schedulerListener = new TransducerPartListener();
        processListener = new TransducerPartListener();
        orderListener = new TransducerPartListener();
    }

    @Override
    public void enterInitialConfigs(InitialConfigsContext ctx)
    {
        ctx.automaton().enterRule(initialConfigsListener);
    }

    @Override
    public void enterFinalConfigs(FinalConfigsContext ctx)
    {
        ctx.automaton().enterRule(finalConfigsListener);
    }

    @Override
    public void enterScheduler(SchedulerContext ctx)
    {
        ctx.transducer().enterRule(schedulerListener);
    }

    @Override
    public void enterProcess(ProcessContext ctx)
    {
        ctx.transducer().enterRule(processListener);
    }

    @Override
    public void enterInvariant(InvariantContext ctx)
    {
        ctx.automaton().enterRule(invariantListener);
    }

    @Override
    public void enterOrder(OrderContext ctx)
    {
        ctx.transducer().enterRule(orderListener);
    }

    private static IntIntPair sortedIntIntPair(int one, int two)
    {
        return one > two ? PrimitiveTuples.pair(two, one) : PrimitiveTuples.pair(one, two);
    }

    @Override
    public void enterInvariantSizeBound(InvariantSizeBoundContext ctx)
    {
        final int from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 0);
        final int to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 0);
        invSize = sortedIntIntPair(from, to);
    }

    @Override
    public void enterOrderSizeBound(OrderSizeBoundContext ctx)
    {
        final int from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 0);
        final int to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 0);
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

    private class FSAPartListener extends ProblemBaseListener
    {
        private final FSAListener<String> impl;

        private FSAPartListener()
        {
            impl = new FSAListener<>(alphabetRecorder);
        }

        private ListIterable<FSA<String>> getAutomata()
        {
            return impl.getAutomata();
        }

        @Override
        public void enterAutomaton(AutomatonContext ctx)
        {
            impl.enterAutomaton(ctx.getStart().getLine(), ctx.getStop().getLine());
            ctx.startStates().enterRule(this);
            ctx.transitions().transition().forEach(transCtx -> transCtx.enterRule(this));
            ctx.acceptStates().enterRule(this);
            ctx.exitRule(this);
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

    private class TransducerPartListener extends ProblemBaseListener
    {
        private final FSAListener<Twin<String>> impl;

        private TransducerPartListener()
        {
            impl = new FSAListener<>(Alphabets.builder(commonCapacity, TRANSDUCER_EPSILON_SYMBOL));
        }

        private ListIterable<FSA<Twin<String>>> getAutomataWith(Alphabet<Twin<String>> override)
        {
            return impl.getAutomataWith(override);
        }

        @Override
        public void enterTransducer(TransducerContext ctx)
        {
            impl.enterAutomaton(ctx.getStart().getLine(), ctx.getStop().getLine());
            ctx.startStates().enterRule(this);
            ctx.transducerTransitions().transducerTransition().forEach(transCtx -> transCtx.enterRule(this));
            ctx.acceptStates().enterRule(this);
            ctx.exitRule(this);
        }

        @Override
        public void exitTransducer(TransducerContext ctx)
        {
            impl.exitAutomaton();
        }

        @Override
        public void enterStartStates(StartStatesContext ctx)
        {
            impl.enterStartStates(ctx.stateList().ID());
        }

        @Override
        public void enterTransducerTransition(TransducerTransitionContext ctx)
        {
            final Twin<String> symbol;
            final TransducerTransitionLabelContext label = ctx.transducerTransitionLabel();
            if (label.epsilonTransitionLabel() != null) {
                symbol = TRANSDUCER_EPSILON_SYMBOL;
            } else {
                final String input = label.monadIOTransitionLabel().ID(0).getText();
                final String output = label.monadIOTransitionLabel().ID(1).getText();
                alphabetRecorder.add(input).add(output);
                symbol = Tuples.twin(input, output);
            }

            impl.enterTransition(ctx.ID(), symbol);
        }

        @Override
        public void enterAcceptStates(AcceptStatesContext ctx)
        {
            impl.enterAcceptStates(ctx.stateList().ID());
        }
    }
}
