package automata.part;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;

import java.util.List;

public class StateImpl implements State
{
    private final int id;
    private final String displayName;
    private int referenceIndex = -1;

    protected int hashCode;
    protected boolean isDeterministic;
    protected ImmutableList<? extends Transition<? extends Label>> dTransitions;
    protected ImmutableList<ImmutableList<? extends Transition<? extends Label>>> nTransitions;

    protected int computeHashCode()
    {
        final int prime = 82483721;
        int result = 15492553;

        result = result * prime + id * prime;
        result = result * prime + (isDeterministic ? 0 : prime);

        return result;
    }

    protected StateImpl(int id, String displayName, int referenceIndex)
    {
        this.id = id;
        this.referenceIndex = referenceIndex;
        this.displayName = displayName;
    }

    public StateImpl(int id, String displayName, ImmutableList<? extends Transition<? extends Label>> dTransitions)
    {
        this.id = id;
        this.displayName = displayName;
        this.dTransitions = dTransitions;
        isDeterministic = true;
        hashCode = computeHashCode(); // FIXME: [anti] overridable method in constructor
    }

    public StateImpl(int id, String displayName,
                     List<? extends ImmutableList<? extends Transition<? extends Label>>> nTransitions)
    {
        this.id = id;
        this.displayName = displayName;
        this.nTransitions = Lists.immutable.ofAll(nTransitions);
        hashCode = computeHashCode();
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
    public int getId()
    {
        return id;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public int getReferenceIndex()
    {
        return referenceIndex;
    }

    @Override
    public ImmutableList<? extends Transition<? extends Label>> getDTransitions()
    {
        return dTransitions;
    }

    @Override
    public ImmutableList<? extends ImmutableList<? extends Transition<? extends Label>>> getNTransitions()
    {
        return nTransitions;
    }

    @Override
    public String toString()
    {
        final String newline = System.getProperty("line.separator");
        final String indent = "  ";
        final StringBuilder s = new StringBuilder();

        if (isDeterministic) {
            dTransitions.select(Predicates.notNull()).forEach(t -> s.append(indent).append(t).append(newline));
        } else {
            nTransitions.select(Predicates.notNull()).forEach(ts -> {
                ts.forEach(t -> s.append(indent).append(t).append(newline));
            });
        }

        return s.toString();
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof State) {
            final State other = (State) obj;

            final boolean sameId = id == other.getId();
            final boolean sameDeterminism = isDeterministic == other.isDeterministic();

            return sameId && sameDeterminism;
        } else {
            return false;
        }
    }
}
