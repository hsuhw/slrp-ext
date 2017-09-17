package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Deterministic;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.MapMapDelta;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.impl.block.factory.primitive.BooleanPredicates;

public class MapMapDFSA<S extends Symbol> extends AbstractAutomaton<S> implements Deterministic, FSA<S>
{
    private final Alphabet<S> alphabet;

    public MapMapDFSA(Alphabet<S> alphabet, ImmutableList<State> states, ImmutableBooleanList startStateTable,
                      ImmutableBooleanList acceptStateTable, MapMapDelta<S> transitionFunction)
    {
        super(states, startStateTable, acceptStateTable, transitionFunction);
        if (startStateTable.count(BooleanPredicates.isTrue()) != 1) {
            throw new IllegalArgumentException("more than one start states specified");
        }
        this.alphabet = alphabet;
    }

    public MapMapDFSA(Alphabet<S> alphabet, MutableList<State> states, MutableBooleanList startStateTable,
                      MutableBooleanList acceptStateTable, MapMapDelta<S> transitionFunction)
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
    public MapMapDelta<S> getTransitionFunction()
    {
        return (MapMapDelta<S>) super.getTransitionFunction();
    }
}
