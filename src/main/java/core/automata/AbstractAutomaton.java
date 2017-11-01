package core.automata;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;
import api.automata.TransitionGraph;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import static api.util.Values.DISPLAY_INDENT;
import static api.util.Values.DISPLAY_NEWLINE;
import static core.util.Parameters.IMPLICIT_PRECONDITION_RESPECTED;

public abstract class AbstractAutomaton<S> implements Automaton<S>
{
    private final Alphabet<S> alphabet;
    private final ImmutableSet<State> states;
    private final ImmutableSet<State> startStates;
    private final ImmutableSet<State> acceptStates;
    private final TransitionGraph<State, S> transitionGraph;

    private ImmutableSet<State> nonStartStates;
    private ImmutableSet<State> nonAcceptStates;
    private SetIterable<State> unreachableStates;
    private SetIterable<State> deadEndStates;
    private SetIterable<State> danglingStates;
    private ImmutableMap<State, String> maskedStateNames;
    private String rawDisplay;
    private String nameMaskedDisplay;

    private static <S> boolean validateDefinition(Alphabet<S> sigma, ImmutableSet<State> states,
                                                  ImmutableSet<State> startStates, ImmutableSet<State> acceptStates,
                                                  TransitionGraph<State, S> transitionGraph)
    {
        final boolean validStartStates = states.containsAllIterable(startStates);
        final boolean validAcceptStates = states.containsAllIterable(acceptStates);
        final boolean validTransSymbols = sigma.set().containsAllIterable(transitionGraph.referredArcLabels());
        final boolean validTransStates = states.containsAllIterable(transitionGraph.referredNodes());
        final boolean validTransGraph = validTransSymbols && validTransStates;
        final boolean atLeastOneStartState = startStates.size() > 0;

        return validStartStates && validAcceptStates && validTransGraph && atLeastOneStartState;
    }

    public AbstractAutomaton(Alphabet<S> alphabet, ImmutableSet<State> states, ImmutableSet<State> startStates,
                             ImmutableSet<State> acceptStates, TransitionGraph<State, S> transitionGraph)
    {
        if (!IMPLICIT_PRECONDITION_RESPECTED // a switch for those children not constructed by a closed builder
            && !validateDefinition(alphabet, states, startStates, acceptStates, transitionGraph)) {
            throw new IllegalArgumentException("invalid definition given");
        }

        this.alphabet = alphabet;
        this.states = states;
        this.startStates = startStates;
        this.acceptStates = acceptStates;
        this.transitionGraph = transitionGraph;
    }

    @Override
    public Alphabet<S> alphabet()
    {
        return alphabet;
    }

    @Override
    public ImmutableSet<State> states()
    {
        return states;
    }

    @Override
    public ImmutableSet<State> startStates()
    {
        return startStates;
    }

    @Override
    public ImmutableSet<State> nonStartStates()
    {
        if (nonStartStates == null) {
            nonStartStates = Automaton.super.nonStartStates();
        }

        return nonStartStates;
    }

    @Override
    public ImmutableSet<State> acceptStates()
    {
        return acceptStates;
    }

    @Override
    public ImmutableSet<State> nonAcceptStates()
    {
        if (nonAcceptStates == null) {
            nonAcceptStates = Automaton.super.nonAcceptStates();
        }

        return nonAcceptStates;
    }

    @Override
    public SetIterable<State> unreachableStates()
    {
        if (unreachableStates == null) {
            unreachableStates = Automaton.super.unreachableStates();
        }

        return unreachableStates;
    }

    @Override
    public SetIterable<State> deadEndStates()
    {
        if (deadEndStates == null) {
            deadEndStates = Automaton.super.deadEndStates();
        }

        return deadEndStates;
    }

    @Override
    public SetIterable<State> danglingStates()
    {
        if (danglingStates == null) {
            danglingStates = Automaton.super.danglingStates();
        }

        return danglingStates;
    }

    @Override
    public TransitionGraph<State, S> transitionGraph()
    {
        return transitionGraph;
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    protected final ImmutableMap<State, String> maskedStateNames()
    {
        if (maskedStateNames == null) {
            final MutableMap<State, String> names = UnifiedMap.newMap(states.size()); // upper bound
            int i = 0;
            for (State generated : states.selectInstancesOf(NamelessState.class)) {
                names.put(generated, BasicStates.GENERATED_PREFIX + i);
                i++;
            }
            maskedStateNames = names.toImmutable();
        }

        return maskedStateNames;
    }

    private String rawDisplay()
    {
        if (rawDisplay == null) {
            rawDisplay = "{" + DISPLAY_NEWLINE //
                + DISPLAY_INDENT + "start: " + startStates.makeString() + ";" + DISPLAY_NEWLINE //
                + DISPLAY_NEWLINE //
                + transitionGraph //
                + DISPLAY_NEWLINE //
                + DISPLAY_INDENT + "accept: " + acceptStates.makeString() + ";" + DISPLAY_NEWLINE //
                + "}" + DISPLAY_NEWLINE;
        }

        return rawDisplay;
    }

    private String determineStateName(State state)
    {
        return state instanceof NamedState ? state.toString() : maskedStateNames.get(state);
    }

    private String nameMaskedDisplay()
    {
        maskedStateNames();
        final String startStateNames = startStates.collect(this::determineStateName).makeString();
        final String acceptStateNames = acceptStates.collect(this::determineStateName).makeString();
        if (nameMaskedDisplay == null) {
            nameMaskedDisplay = "{" + DISPLAY_NEWLINE //
                + DISPLAY_INDENT + "start: " + startStateNames + ";" + DISPLAY_NEWLINE //
                + DISPLAY_NEWLINE //
                + transitionGraph.toString(maskedStateNames) //
                + DISPLAY_NEWLINE //
                + DISPLAY_INDENT + "accept: " + acceptStateNames + ";" + DISPLAY_NEWLINE //
                + "}" + DISPLAY_NEWLINE;
        }

        return nameMaskedDisplay;
    }

    @Override
    public String toString(boolean maskStateNames)
    {
        return maskStateNames ? nameMaskedDisplay() : rawDisplay();
    }
}
