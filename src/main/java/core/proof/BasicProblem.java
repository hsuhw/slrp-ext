package core.proof;

import api.automata.fsa.FSA;
import api.proof.Problem;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public class BasicProblem<S> implements Problem<S>
{
    private final FSA<S> initialConfigs;
    private final FSA<S> finalConfigs;
    private final FSA<Twin<S>> scheduler;
    private final FSA<Twin<S>> process;
    private final FSA<S> invariant;
    private final FSA<Twin<S>> order;
    private final IntIntPair invariantSizeBound;
    private final IntIntPair orderSizeBound;
    private final boolean invariantEnclosesAllBehavior;

    public BasicProblem(FSA<S> initialConfigs, FSA<S> finalConfigs, FSA<Twin<S>> scheduler, FSA<Twin<S>> process,
                        FSA<S> invariant, FSA<Twin<S>> order, IntIntPair invariantSizeBound, IntIntPair orderSizeBound,
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
    public FSA<Twin<S>> scheduler()
    {
        return scheduler;
    }

    @Override
    public FSA<Twin<S>> process()
    {
        return process;
    }

    @Override
    public FSA<S> invariant()
    {
        return invariant;
    }

    @Override
    public FSA<Twin<S>> order()
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
