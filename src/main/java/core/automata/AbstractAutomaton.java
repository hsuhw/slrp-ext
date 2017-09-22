package core.automata;

import api.automata.Automaton;
import api.automata.DeltaFunction;
import api.automata.State;
import org.eclipse.collections.api.set.ImmutableSet;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;
import static core.util.Parameters.IMPLICIT_PRECONDITION_RESPECTED;

public abstract class AbstractAutomaton<S> implements Automaton<S>
{
    protected final ImmutableSet<State> states;
    protected final ImmutableSet<State> startStates;
    protected final ImmutableSet<State> acceptStates;
    protected final DeltaFunction<S> deltaFunction;

    protected static <S> boolean validateDefinition(ImmutableSet<State> states, ImmutableSet<State> startStates,
                                                    ImmutableSet<State> acceptStates, DeltaFunction<S> deltaFunction)
    {
        final boolean validStartStates = states.containsAll(startStates.castToSet());
        final boolean validAcceptStates = startStates.containsAll(acceptStates.castToSet());
        final boolean validDeltaFunction;
        final boolean validDeltaFunctionStates = states.containsAll(deltaFunction.getAllReferredStates().toSet());
        if (!IMPLICIT_PRECONDITION_RESPECTED) {
            validDeltaFunction = validDeltaFunctionStates && deltaFunction.size() > 0;
        } else {
            validDeltaFunction = validDeltaFunctionStates;
        }
        final boolean atLeastOneStartState = startStates.size() > 0;

        return validStartStates && validAcceptStates && validDeltaFunction && atLeastOneStartState;
    }

    public AbstractAutomaton(ImmutableSet<State> states, ImmutableSet<State> startStates,
                             ImmutableSet<State> acceptStates, DeltaFunction<S> deltaFunction)
    {
        if (!validateDefinition(states, startStates, acceptStates, deltaFunction)) {
            throw new IllegalArgumentException("given an invalid definition");
        }
        this.states = states;
        this.startStates = startStates;
        this.acceptStates = acceptStates;
        this.deltaFunction = deltaFunction;
    }

    @Override
    public ImmutableSet<State> getStates()
    {
        return states;
    }

    @Override
    public ImmutableSet<State> getStartStates()
    {
        return startStates;
    }

    @Override
    public ImmutableSet<State> getAcceptStates()
    {
        return acceptStates;
    }

    @Override
    public DeltaFunction<S> getDeltaFunction()
    {
        return deltaFunction;
    }

    @Override
    public String toString()
    {
        return "{" + DISPLAY_NEWLINE //
            + DISPLAY_INDENT + "start: " + startStates + ";" + DISPLAY_NEWLINE //
            + DISPLAY_NEWLINE //
            + deltaFunction //
            + DISPLAY_NEWLINE //
            + DISPLAY_INDENT + "accept: " + acceptStates + ";" + DISPLAY_NEWLINE //
            + "}" + DISPLAY_NEWLINE;
    }
}
