package automata;

import automata.part.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;

public abstract class AutomatonAbst implements Automaton
{
    protected final ImmutableList<State> states;
    protected final ImmutableBooleanList startStateTable;
    protected final ImmutableBooleanList acceptStateTable;
    protected final boolean isDeterministic;

    public AutomatonAbst(ImmutableList<State> states, ImmutableBooleanList startStateTable,
                         ImmutableBooleanList acceptStateTable)
    {
        this.states = states;
        this.startStateTable = startStateTable;
        this.acceptStateTable = acceptStateTable;
        this.isDeterministic = states.allSatisfy(State::isDeterministic);
    }

    @Override
    public boolean isDeterministic()
    {
        return isDeterministic;
    }

    @Override
    public boolean isNondeterministic()
    {
        return !isDeterministic;
    }

    @Override
    public ImmutableList<State> getStates()
    {
        return states;
    }

    @Override
    public ImmutableBooleanList getStartStateTable()
    {
        return startStateTable;
    }

    @Override
    public boolean isStartState(int stateIndex)
    {
        return startStateTable.get(stateIndex);
    }

    @Override
    public ImmutableBooleanList getAcceptStateTable()
    {
        return acceptStateTable;
    }

    @Override
    public boolean isAcceptState(int stateIndex)
    {
        return acceptStateTable.get(stateIndex);
    }

    @Override
    public String toString()
    {
        final String newline = System.getProperty("line.separator");
        final String indent = "  ";
        final StringBuilder sout = new StringBuilder();

        sout.append("{").append(newline);
        sout.append(indent).append("start:");
        startStateTable.forEachWithIndex((x, i) -> {
            if (x) {
                sout.append(" s").append(i);
            }
        });
        sout.append(";").append(newline);
        states.forEach(sout::append);
        sout.append(indent).append("accept:");
        acceptStateTable.forEachWithIndex((x, i) -> {
            if (x) {
                sout.append(" s").append(i);
            }
        });
        sout.append(";").append(newline);
        sout.append("}").append(newline);

        return sout.toString();
    }

    // TODO: [lack] hashCode & equals
}
