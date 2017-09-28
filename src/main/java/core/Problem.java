package core;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

public class Problem
{
    private final FSA<String> initialConfigurations;
    private final FSA<String> finalConfigurations;
    private final FSA<Twin<String>> schedulerBehavior;
    private final FSA<Twin<String>> processBehavior;
    private final IntIntPair invariantConfigSearchSpace;
    private final IntIntPair orderRelationSearchSpace;

    private static IntIntPair sortIntIntPair(IntIntPair pair)
    {
        final int one = pair.getOne();
        final int two = pair.getTwo();
        return one > two ? PrimitiveTuples.pair(two, one) : pair;
    }

    public Problem(FSA<String> initialConfig, FSA<String> finalConfig, FSA<Twin<String>> player1,
                   FSA<Twin<String>> player2, IntIntPair invariantSearchSpace, IntIntPair relationSearchSpace)
    {
        initialConfigurations = initialConfig;
        finalConfigurations = finalConfig;
        schedulerBehavior = player1;
        processBehavior = player2;
        invariantConfigSearchSpace = sortIntIntPair(invariantSearchSpace);
        orderRelationSearchSpace = sortIntIntPair(relationSearchSpace);
    }

    public FSA<String> getInitialConfigurations()
    {
        return initialConfigurations;
    }

    public FSA<String> getFinalConfigurations()
    {
        return finalConfigurations;
    }

    public FSA<Twin<String>> getSchedulerBehavior()
    {
        return schedulerBehavior;
    }

    public FSA<Twin<String>> getProcessBehavior()
    {
        return processBehavior;
    }

    public IntIntPair getInvariantConfigSearchSpace()
    {
        return invariantConfigSearchSpace;
    }

    public IntIntPair getOrderRelationSearchSpace()
    {
        return orderRelationSearchSpace;
    }
}
