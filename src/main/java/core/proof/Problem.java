package core.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public class Problem
{
    private final FSA<String> initialConfigurations;
    private final FSA<String> finalConfigurations;
    private final FSA<String> invariant;
    private final FSA<Twin<String>> schedulerBehavior;
    private final FSA<Twin<String>> processBehavior;
    private final FSA<Twin<String>> orderRelation;
    private final IntIntPair invariantSizeBound;
    private final IntIntPair orderRelationSizeBound;

    public Problem(FSA<String> initialConfigs, FSA<String> finalConfigs, FSA<String> invariant,
                   FSA<Twin<String>> scheduler, FSA<Twin<String>> process, FSA<Twin<String>> relation,
                   IntIntPair invariantBound, IntIntPair relationBound)
    {
        initialConfigurations = initialConfigs;
        finalConfigurations = finalConfigs;
        this.invariant = invariant;
        schedulerBehavior = scheduler;
        processBehavior = process;
        orderRelation = relation;
        invariantSizeBound = invariantBound;
        orderRelationSizeBound = relationBound;
    }

    public FSA<String> initialConfigurations()
    {
        return initialConfigurations;
    }

    public FSA<String> finalConfigurations()
    {
        return finalConfigurations;
    }

    public FSA<String> invariant()
    {
        return invariant;
    }

    public FSA<Twin<String>> schedulerBehavior()
    {
        return schedulerBehavior;
    }

    public FSA<Twin<String>> processBehavior()
    {
        return processBehavior;
    }

    public FSA<Twin<String>> orderRelation()
    {
        return orderRelation;
    }

    public IntIntPair invariantSizeBound()
    {
        return invariantSizeBound;
    }

    public IntIntPair orderRelationSizeBound()
    {
        return orderRelationSizeBound;
    }
}
