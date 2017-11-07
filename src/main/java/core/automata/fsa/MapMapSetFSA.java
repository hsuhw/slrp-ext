package core.automata.fsa;

import api.automata.State;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.MapMapSetGraph;
import org.eclipse.collections.api.set.ImmutableSet;

public class MapMapSetFSA<S> extends AbstractAutomaton<S> implements FSA<S>
{
    private ImmutableSet<State> incompleteStates;
    private Boolean acceptsNone;

    public MapMapSetFSA(MapMapSetFSABuilder<S> record)
    {
        super(record.exportAlphabet(), record.states(), record.startStates(), record.acceptStates(),
              record.exportDelta());
    }

    @Override
    public MapMapSetGraph<State, S> transitionGraph()
    {
        return (MapMapSetGraph<State, S>) super.transitionGraph();
    }

    @Override
    public ImmutableSet<State> incompleteStates()
    {
        if (incompleteStates == null) {
            incompleteStates = FSA.super.incompleteStates();
        }

        return incompleteStates;
    }

    @Override
    public boolean acceptsNone()
    {
        if (acceptsNone == null) {
            acceptsNone = FSA.super.acceptsNone();
        }

        return acceptsNone;
    }
}
