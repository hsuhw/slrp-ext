package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.DeltaFunction;
import api.automata.Deterministic;
import api.automata.State;
import api.automata.fsa.FSA;
import core.automata.AbstractAutomaton;
import core.automata.MapMapDelta;
import org.eclipse.collections.api.set.ImmutableSet;

public final class MapMapDFSA<S> extends AbstractAutomaton<S> implements Deterministic, FSA<S>
{
    private MapMapDFSA(Alphabet<S> sigma, ImmutableSet<State> states, ImmutableSet<State> startStates,
                       ImmutableSet<State> acceptStates, DeltaFunction<S> delta)
    {
        super(sigma, states, startStates, acceptStates, delta);
        if (startStates.size() != 1) {
            throw new IllegalArgumentException("more than one start states specified");
        }
    }

    public MapMapDFSA(BasicFSABuilder<S> record)
    {
        this(record.getExportingAlphabet(), record.getStates(), record.getStartStates(), record.getAcceptStates(),
             record.getExportingDelta());
    }

    @Override
    public MapMapDelta<S> getDeltaFunction()
    {
        return (MapMapDelta<S>) super.getDeltaFunction();
    }
}
