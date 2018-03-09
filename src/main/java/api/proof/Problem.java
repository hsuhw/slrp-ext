package api.proof;

import api.automata.fsa.FSA;
import api.automata.fst.FST;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;

public interface Problem<S>
{
    FSA<S> initialConfigs();

    FSA<S> finalConfigs();

    FST<S, S> scheduler();

    FST<S, S> process();

    FSA<S> invariant();

    FST<S, S> order();

    IntIntPair invariantSizeBound();

    IntIntPair orderSizeBound();

    boolean invariantEnclosesAllBehavior();
}
