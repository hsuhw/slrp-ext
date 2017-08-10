package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Deterministic;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.DoubleMapDelta;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public class DoubleMapDFSA<S extends Symbol> extends AbstractAutomaton<S> implements Deterministic, FSA<S>
{
    private final Alphabet<S> alphabet;

    public DoubleMapDFSA(Alphabet<S> alphabet, ImmutableList<State> states, ImmutableBooleanList startStateTable,
                         ImmutableBooleanList acceptStateTable, DoubleMapDelta<S> transitionFunction)
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
    public DoubleMapDelta<S> getTransitionFunction()
    {
        return (DoubleMapDelta<S>) super.getTransitionFunction();
    }
}
