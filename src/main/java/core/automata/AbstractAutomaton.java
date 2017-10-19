package core.automata;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.State;
import api.automata.TransitionGraph;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
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
    private boolean generatedNamesSettled;
    private ImmutableMap<State, String> generatedStateNames;
    private ImmutableSet<String> startStatesDisplay;
    private ImmutableSet<String> acceptStatesDislplay;

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
    public ImmutableSet<State> acceptStates()
    {
        return acceptStates;
    }

    @Override
    public TransitionGraph<State, S> transitionGraph()
    {
        return transitionGraph;
    }

    private String getStateNameOrSettleOne(State state)
    {
        return state instanceof NamedState ? state.toString() : generatedStateNames.get(state);
    }

    protected final ImmutableMap<State, String> generatedStateNames()
    {
        if (!generatedNamesSettled) {
            final MutableMap<State, String> map = UnifiedMap.newMap(states.size()); // upper bound
            int i = 0;
            for (State generated : states.selectInstancesOf(NamelessState.class)) {
                map.put(generated, BasicStates.GENERATED_PREFIX + i);
                i++;
            }
            generatedStateNames = map.toImmutable();
            startStatesDisplay = startStates.collect(this::getStateNameOrSettleOne);
            acceptStatesDislplay = acceptStates.collect(this::getStateNameOrSettleOne);
        }

        return generatedStateNames;
    }

    @Override
    public String toString()
    {
        if (generatedStateNames == null) {
            generatedStateNames = generatedStateNames();
        }

        return "{" + DISPLAY_NEWLINE //
            + DISPLAY_INDENT + "start: " + startStatesDisplay + ";" + DISPLAY_NEWLINE //
            + DISPLAY_NEWLINE //
            + transitionGraph.toString(generatedStateNames) //
            + DISPLAY_NEWLINE //
            + DISPLAY_INDENT + "accept: " + acceptStatesDislplay + ";" + DISPLAY_NEWLINE //
            + "}" + DISPLAY_NEWLINE;
    }
}
