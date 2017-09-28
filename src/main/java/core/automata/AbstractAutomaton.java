package core.automata;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.DeltaFunction;
import api.automata.State;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;
import static core.util.Parameters.IMPLICIT_PRECONDITION_RESPECTED;

public abstract class AbstractAutomaton<S> implements Automaton<S>
{
    protected final Alphabet<S> alphabet;
    protected final ImmutableSet<State> states;
    protected final ImmutableSet<State> startStates;
    protected final ImmutableSet<State> acceptStates;
    protected final DeltaFunction<S> deltaFunction;

    protected static <S> boolean validateDefinition(Alphabet<S> sigma, ImmutableSet<State> states,
                                                    ImmutableSet<State> startStates, ImmutableSet<State> acceptStates,
                                                    DeltaFunction<S> deltaFunction)
    {
        final boolean validStartStates = states.containsAllIterable(startStates);
        final boolean validAcceptStates = startStates.containsAllIterable(acceptStates);
        final SetIterable<S> deltaSymbols = deltaFunction.getAllReferredSymbols();
        final SetIterable<State> deltaStates = deltaFunction.getAllReferredStates();
        final boolean validDeltaFunctionSymbols = sigma.getSet().containsAllIterable(deltaSymbols);
        final boolean validDeltaFunctionStates = states.containsAllIterable(deltaStates);
        final boolean validDeltaFunction = validDeltaFunctionSymbols && validDeltaFunctionStates;
        final boolean atLeastOneStartState = startStates.size() > 0;

        return validStartStates && validAcceptStates && validDeltaFunction && atLeastOneStartState;
    }

    public AbstractAutomaton(Alphabet<S> sigma, ImmutableSet<State> states, ImmutableSet<State> startStates,
                             ImmutableSet<State> acceptStates, DeltaFunction<S> deltaFunction)
    {
        if (!IMPLICIT_PRECONDITION_RESPECTED // not constructed through a builder
            && !validateDefinition(sigma, states, startStates, acceptStates, deltaFunction)) {
            throw new IllegalArgumentException("given an invalid definition");
        }

        alphabet = sigma;
        this.states = states;
        this.startStates = startStates;
        this.acceptStates = acceptStates;
        this.deltaFunction = deltaFunction;
    }

    @Override
    public Alphabet<S> getAlphabet()
    {
        return alphabet;
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
