package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Nondeterministic;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.MapMapSetDelta;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;

public class MapMapSetNFSA<S extends Symbol> extends AbstractAutomaton<S> implements Nondeterministic, FSA<S>
{
    private final Alphabet<S> alphabet;

    public MapMapSetNFSA(Alphabet<S> alphabet, ImmutableList<State> states, ImmutableBooleanList startStateTable,
                         ImmutableBooleanList acceptStateTable, MapMapSetDelta<S> transitionFunction)
    {
        super(states, startStateTable, acceptStateTable, transitionFunction);
        this.alphabet = alphabet;
    }

    public MapMapSetNFSA(Alphabet<S> alphabet, MutableList<State> states, MutableBooleanList startStateTable,
                         MutableBooleanList acceptStateTable, MapMapSetDelta<S> transitionFunction)
    {
        this(alphabet, states.toImmutable(), startStateTable.toImmutable(), acceptStateTable.toImmutable(),
             transitionFunction);
    }

    @Override
    public Alphabet<S> getAlphabet()
    {
        return alphabet;
    }

    @Override
    public MapMapSetDelta<S> getTransitionFunction()
    {
        return (MapMapSetDelta<S>) super.getTransitionFunction();
    }
}
