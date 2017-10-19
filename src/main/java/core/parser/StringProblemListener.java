package core.parser;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import core.Problem;
import core.parser.fsa.FSAListener;
import generated.ProblemBaseListener;
import generated.ProblemParser;
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
    private FSAPartListener initialConfigListener;
    private FSAPartListener finalConfigListener;
    private TransducerPartListener schedulerListener;
    private TransducerPartListener processListener;
    private IntIntPair invariantBound;
    private IntIntPair relationBound;

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
    public void enterProblem(ProblemContext ctx)
    {
        commonCapacity = (ctx.getStop().getLine() - ctx.getStart().getLine()) / 4; // loose upper bound
        final Alphabet.Builder<String> alphabetRecorder = Alphabets.builder(commonCapacity, EPSILON_SYMBOL);
        initialConfigListener = new FSAPartListener(alphabetRecorder);
        finalConfigListener = new FSAPartListener(alphabetRecorder);
        schedulerListener = new TransducerPartListener();
        processListener = new TransducerPartListener();
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
        final int from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 1);
        final int to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 1);
        invariantBound = PrimitiveTuples.pair(from, to);
    }

    @Override
    public void enterRelationSearchSpace(ProblemParser.RelationSearchSpaceContext ctx)
    {
        final int from = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(0).getText()), 1);
        final int to = Math.max(Integer.parseInt(ctx.integerRange().INTEGER(1).getText()), 1);
        relationBound = PrimitiveTuples.pair(from, to);
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
