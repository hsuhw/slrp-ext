package core.automata;

import api.automata.Automaton;
import api.automata.State;
import api.automata.Symbol;
import api.automata.TransitionFunction;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.block.factory.primitive.BooleanPredicates;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public abstract class AbstractAutomaton<S extends Symbol> implements Automaton<S>
{
    protected final ImmutableList<State> states;
    protected final ImmutableBooleanList startStateTable;
    protected final ImmutableBooleanList acceptStateTable;
    protected final ImmutableObjectIntMap<State> stateIndexTable;
    protected final TransitionFunction<S> transitionFunction;

    private static <S extends Symbol> boolean meetsMinimumRequirements(ImmutableList<State> states,
                                                                       ImmutableBooleanList startStateTable,
                                                                       ImmutableBooleanList acceptStateTable,
                                                                       TransitionFunction<S> transitionFunction)
    {
        final int stateNumber = states.size();
        final int startTableSize = startStateTable.size();
        final int acceptTableSize = acceptStateTable.size();
        final boolean consistentStateNumber = startTableSize == stateNumber && acceptTableSize == stateNumber;
        final boolean atLeastOneTransition = transitionFunction.size() > 0;
        final boolean atLeastOneStartState = startStateTable.anySatisfy(BooleanPredicates.isTrue());

        return consistentStateNumber && atLeastOneTransition && atLeastOneStartState;
    }

    public AbstractAutomaton(ImmutableList<State> states, ImmutableBooleanList startStateTable,
                             ImmutableBooleanList acceptStateTable, TransitionFunction<S> transitionFunction)
    {
        if (!meetsMinimumRequirements(states, startStateTable, acceptStateTable, transitionFunction)) {
            throw new IllegalArgumentException("given definition does not meet minimum requirements");
        }
        final MutableObjectIntMap<State> stateIndexTable = new ObjectIntHashMap<>(states.size());
        states.forEachWithIndex(stateIndexTable::put);
        // TODO: see whether to prevent the map creation when possible
        this.states = states;
        this.stateIndexTable = stateIndexTable.toImmutable();
        this.startStateTable = startStateTable;
        this.acceptStateTable = acceptStateTable;
        this.transitionFunction = transitionFunction;
    }

    @Override
    public ImmutableList<State> getStates()
    {
        return states;
    }

    @Override
    public ImmutableObjectIntMap<State> getStateIndexTable()
    {
        return stateIndexTable;
    }

    @Override
    public ImmutableBooleanList getStartStateTable()
    {
        return startStateTable;
    }

    @Override
    public ImmutableBooleanList getAcceptStateTable()
    {
        return acceptStateTable;
    }

    @Override
    public TransitionFunction<S> getTransitionFunction()
    {
        return transitionFunction;
    }

    @Override
    public String toString()
    {
        final String newline = System.getProperty("line.separator");
        final String indent = "  ";
        final StringBuilder layout = new StringBuilder();

        layout.append("{").append(newline);
        layout.append(indent).append("start:");
        startStateTable.forEachWithIndex((x, i) -> {
            if (x) {
                layout.append(" s").append(i);
            }
        });
        layout.append(";").append(newline).append(newline);
        layout.append(transitionFunction).append(newline);
        layout.append(indent).append("accept:");
        acceptStateTable.forEachWithIndex((x, i) -> {
            if (x) {
                layout.append(" s").append(i);
            }
        });
        layout.append(";").append(newline);
        layout.append("}").append(newline);

        return layout.toString();
    }
}
