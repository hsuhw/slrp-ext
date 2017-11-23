package api.proof;

import api.automata.fsa.FSA;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public interface Problem<S>
{
    FSA<S> initialConfigs();

    FSA<S> finalConfigs();

    FSA<Twin<S>> scheduler();

    FSA<Twin<S>> process();

    FSA<S> invariant();

    FSA<Twin<S>> order();

    IntIntPair invariantSizeBound();

    IntIntPair orderSizeBound();

    boolean invariantEnclosesAllBehavior();
}
