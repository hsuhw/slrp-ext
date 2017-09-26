package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.DeltaFunction;
import api.automata.Nondeterministic;
import api.automata.State;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.MapMapSetDelta;
import org.eclipse.collections.api.set.ImmutableSet;

public final class MapMapSetNFSA<S> extends AbstractAutomaton<S> implements Nondeterministic, FSA<S>
{
    private MapMapSetNFSA(Alphabet<S> sigma, ImmutableSet<State> states, ImmutableSet<State> startStates,
                          ImmutableSet<State> acceptStates, DeltaFunction<S> delta)
    {
        super(sigma, states, startStates, acceptStates, delta);
    }

    public MapMapSetNFSA(BasicFSABuilder<S> record)
    {
        this(record.getExportingAlphabet(), record.getStates(), record.getStartStates(), record.getAcceptStates(),
             record.getExportingDelta());
    }

    @Override
    public MapMapSetDelta<S> getDeltaFunction()
    {
        return (MapMapSetDelta<S>) super.getDeltaFunction();
    }
}
