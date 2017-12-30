package core.proof;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.TransitionGraph;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.proof.Problem;
import api.proof.Prover;
import api.common.sat.SatSolver;
import core.common.sat.Sat4jSolverAdapter;
import org.eclipse.collections.api.block.function.primitive.IntIntToObjectFunction;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public abstract class AbstractProver<S> implements Prover
{
    protected final FSA<S> initialConfigs;
    protected final FSA<S> finalConfigs;
    protected final FSA<S> nonfinalConfigs;
    protected final FSA<Twin<S>> scheduler;
    protected final FSA<Twin<S>> process;
    protected final FSA<S> givenInvariant;
    protected final FSA<Twin<S>> givenOrder;
    protected final Alphabet<S> allAlphabet;
    protected final Alphabet<S> steadyAlphabet;
    protected final Alphabet<Twin<S>> orderAlphabet;
    protected int invariantSizeBegin;
    protected final int invariantSizeEnd;
    protected int orderSizeBegin;
    protected final int orderSizeEnd;
    protected final SatSolver solver;

    protected boolean automataShaped;

    public AbstractProver(Problem<S> problem, boolean shapesAutomata)
    {
        initialConfigs = FSAs.minimize(FSAs.determinize(problem.initialConfigs()));
        finalConfigs = FSAs.minimize(FSAs.determinize(problem.finalConfigs()));
        nonfinalConfigs = FSAs.complement(finalConfigs);
        scheduler = problem.scheduler();
        process = problem.process();
        givenInvariant = problem.invariant();
        givenOrder = problem.order();

        allAlphabet = initialConfigs.alphabet();
        final TransitionGraph<State, Twin<S>> rawProcDelta = problem.process().transitionGraph();
        final ImmutableSet<S> steadySymbols = rawProcDelta.referredArcLabels().collect(Twin::getTwo);
        steadyAlphabet = Alphabets.create(steadySymbols.newWith(allAlphabet.epsilon()), allAlphabet.epsilon());
        orderAlphabet = Alphabets.product(steadyAlphabet);

        final IntIntPair invSizeBound = problem.invariantSizeBound();
        final IntIntPair ordSizeBound = problem.orderSizeBound();
        invariantSizeBegin = invSizeBound != null ? invSizeBound.getOne() : 0;
        invariantSizeEnd = invSizeBound != null ? invSizeBound.getTwo() : 0;
        orderSizeBegin = ordSizeBound != null ? ordSizeBound.getOne() : 0;
        orderSizeEnd = ordSizeBound != null ? ordSizeBound.getTwo() : 0;

        solver = new Sat4jSolverAdapter();

        automataShaped = shapesAutomata;
    }

    protected void search(IntIntToObjectFunction<Pair<FSA<S>, FSA<Twin<S>>>> resultSupplier)
    {
        Pair<FSA<S>, FSA<Twin<S>>> result;
        final int stabilizerBound = invariantSizeEnd * invariantSizeEnd + orderSizeEnd * orderSizeEnd;
        final long startTime = System.currentTimeMillis();
        for (int stabilizer = 1; stabilizer <= stabilizerBound; stabilizer++) {
            for (int invSize = invariantSizeBegin; invSize <= invariantSizeEnd; invSize++) {
                for (int ordSize = orderSizeBegin; ordSize <= orderSizeEnd; ordSize++) {
                    final int stabilizerFactor = invSize * invSize + ordSize * ordSize;
                    if (stabilizerFactor != stabilizer) {
                        continue;
                    }
                    if ((result = resultSupplier.value(invSize, ordSize)) != null) {
                        final long endTime = System.currentTimeMillis();
                        final long timeSpent = endTime - startTime;
                        System.out.println("A proof found under the search bound in " + timeSpent + "ms.");
                        System.out.println();
                        System.out.println("A " + result.getOne());
                        System.out.println("T (>) " + result.getTwo());
                        return;
                    }
                }
            }
        }
        final long endTime = System.currentTimeMillis();
        final long timeSpent = endTime - startTime;
        System.out.println("No proof found under the search bound.  " + timeSpent + "ms spent.");
    }
}
