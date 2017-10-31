package core.parser;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import core.parser.fsa.FSAListener;
import core.proof.Problem;
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
    private FSAPartListener initialConfigsListener;
    private FSAPartListener finalConfigsListener;
    private FSAPartListener invariantListener;
    private TransducerPartListener schedulerListener;
    private TransducerPartListener processListener;
    private TransducerPartListener relationListener;
    private IntIntPair invariantBound;
    private IntIntPair relationBound;

    public ListIterable<Problem> getProblem()
    {
        final FSA<String> initCfg = initialConfigsListener.getAutomata().getOnly();
        final FSA<String> finalCfg = finalConfigsListener.getAutomata().getOnly();
        final ListIterable<FSA<String>> invParsed = invariantListener.getAutomata();
        final FSA<String> inv = invParsed.notEmpty() ? invParsed.getOnly() : null;
        final FSA<Twin<String>> sched = schedulerListener.getAutomata().getOnly();
        final FSA<Twin<String>> proc = processListener.getAutomata().getOnly();
        final ListIterable<FSA<Twin<String>>> relParsed = relationListener.getAutomata();
        final FSA<Twin<String>> rel = relParsed.notEmpty() ? relParsed.getOnly() : null;
        final Problem problem = new Problem(initCfg, finalCfg, inv, sched, proc, rel, invariantBound, relationBound);

        return Lists.immutable.of(problem);
    }

    @Override
    public void enterProblem(ProblemContext ctx)
    {
        commonCapacity = (ctx.getStop().getLine() - ctx.getStart().getLine()) / 4; // loose upper bound
        final Alphabet.Builder<String> alphabetRecorder = Alphabets.builder(commonCapacity, EPSILON_SYMBOL);
        initialConfigsListener = new FSAPartListener(alphabetRecorder);
        finalConfigsListener = new FSAPartListener(alphabetRecorder);
        invariantListener = new FSAPartListener(alphabetRecorder);
        schedulerListener = new TransducerPartListener();
        processListener = new TransducerPartListener();
        relationListener = new TransducerPartListener();
    }

    @Override
    public void enterInitialStatesRepr(InitialStatesReprContext ctx)
    {
        ctx.automaton().enterRule(initialConfigsListener);
    }

    @Override
    public void enterFinalStatesRepr(FinalStatesReprContext ctx)
    {
        ctx.automaton().enterRule(finalConfigsListener);
    }

    @Override
    public void enterSchedulerRepr(SchedulerReprContext ctx)
    {
        ctx.transducer().enterRule(schedulerListener);
    }

    @Override
    public void enterProcessRepr(ProcessReprContext ctx)
    {
        ctx.transducer().enterRule(processListener);
    }

    @Override
    public void enterInvariantRepr(InvariantReprContext ctx)
    {
        ctx.automaton().enterRule(invariantListener);
    }

    @Override
    public void enterRelationRepr(RelationReprContext ctx)
    {
        ctx.transducer().enterRule(relationListener);
    }

    private static IntIntPair sortedIntIntPair(int one, int two)
    {
        return one > two ? PrimitiveTuples.pair(two, one) : PrimitiveTuples.pair(one, two);
    }

    @Override
    public void enterInvariantSearchSpace(InvariantSearchSpaceContext ctx)
    {
        final int from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 1);
        final int to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 1);
        invariantBound = sortedIntIntPair(from, to);
    }

    @Override
    public void enterRelationSearchSpace(RelationSearchSpaceContext ctx)
    {
        final int from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 1);
        final int to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 1);
        relationBound = sortedIntIntPair(from, to);
    }

    private class FSAPartListener extends ProblemBaseListener
    {
        private final FSAListener<String> impl;

        private FSAPartListener(Alphabet.Builder<String> alphabetBuilder)
        {
            impl = new FSAListener<>(alphabetBuilder);
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

        private ListIterable<FSA<Twin<String>>> getAutomata()
        {
            return impl.getAutomata();
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
