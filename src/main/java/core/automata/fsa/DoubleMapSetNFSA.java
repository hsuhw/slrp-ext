package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Nondeterministic;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.DoubleMapSetDelta;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public class DoubleMapSetNFSA<S extends Symbol> extends AbstractAutomaton<S> implements Nondeterministic, FSA<S>
{
    private final Alphabet<S> alphabet;

    public DoubleMapSetNFSA(Alphabet<S> alphabet, ImmutableList<State> states, ImmutableBooleanList startStateTable,
                            ImmutableBooleanList acceptStateTable, DoubleMapSetDelta<S> transitionFunction)
    {
        super(states, startStateTable, acceptStateTable, transitionFunction);
        this.alphabet = alphabet;
    }

    @Override
    public Alphabet<S> getAlphabet()
    {
        return alphabet;
    }

    @Override
    public DoubleMapSetDelta<S> getTransitionFunction()
    {
        return (DoubleMapSetDelta<S>) super.getTransitionFunction();
    }
}
