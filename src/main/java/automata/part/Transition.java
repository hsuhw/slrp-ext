package automata.part;

public final class Transition<L extends Label>
{
    private final State from;
    private final State to;
    private final L label;
    private final int hashCode;

    public Transition(State from, State to, L label)
    {
        this.from = from;
        this.to = to;
        this.label = label;
        hashCode = computeHashCode();
    }

    private int computeHashCode()
    {
        final int prime = 77251609;
        int result = 39012931;

        result = prime * result + (from == null ? 0 : from.hashCode());
        result = prime * result + (to == null ? 0 : to.hashCode());
        result = prime * result + (label == null ? 0 : label.hashCode());

        return result;
    }

    public State getFrom()
    {
        return from;
    }

    public State getTo()
    {
        return to;
    }

    public L getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        final String from = "s" + this.from.getId();
        final String to = "s" + this.to.getId();
        final String label = String.valueOf(this.label);

        return from + " -> " + to + " [" + label + "];";
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        try {
            final Transition<? extends Label> other = (Transition<? extends Label>) obj;

            final boolean sameFrom = from.equals(other.getFrom());
            final boolean sameTo = to.equals(other.getTo());
            final boolean sameLabel = label.equals(other.getLabel());

            return sameFrom && sameTo && sameLabel;
        } catch (ClassCastException e) {
            return false;
        }
    }
}
