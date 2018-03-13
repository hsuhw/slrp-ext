package core.proof;

import api.automata.fsa.FSA;
import api.automata.fst.FST;
import api.proof.Problem;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public class BasicProblem<S> implements Problem<S>
{
    private final FSA<S> initialConfigs;
    private final FSA<S> finalConfigs;
    private final FST<S, S> scheduler;
    private final FST<S, S> process;
    private final FSA<S> invariant;
    private final FST<S, S> order;
    private final IntIntPair invariantSizeBound;
    private final IntIntPair orderSizeBound;
    private final boolean invariantEnclosesAllBehavior;

    public BasicProblem(FSA<S> initialConfigs, FSA<S> finalConfigs, FST<S, S> scheduler, FST<S, S> process,
        FSA<S> invariant, FST<S, S> order, IntIntPair invariantSizeBound, IntIntPair orderSizeBound,
        boolean invariantEnclosesAll)
    {
        this.initialConfigs = initialConfigs;
        this.finalConfigs = finalConfigs;
        this.scheduler = scheduler;
        this.process = process;
        this.invariant = invariant;
        this.order = order;
        this.invariantSizeBound = invariantSizeBound;
        this.orderSizeBound = orderSizeBound;
        invariantEnclosesAllBehavior = invariantEnclosesAll;
    }

    @Override
    public FSA<S> initialConfigs()
    {
        return initialConfigs;
    }

    @Override
    public FSA<S> finalConfigs()
    {
        return finalConfigs;
    }

    @Override
    public FST<S, S> scheduler()
    {
        return scheduler;
    }

    @Override
    public FST<S, S> process()
    {
        return process;
    }

    @Override
    public FSA<S> invariant()
    {
        return invariant;
    }

    @Override
    public FST<S, S> order()
    {
        return order;
    }

    @Override
    public IntIntPair invariantSizeBound()
    {
        return invariantSizeBound;
    }

    @Override
    public IntIntPair orderSizeBound()
    {
        return orderSizeBound;
    }

    @Override
    public boolean invariantEnclosesAllBehavior()
    {
        return invariantEnclosesAllBehavior;
    }
}
