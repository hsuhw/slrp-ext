package automata.part;

public class LabelImpl implements Label
{
    private final String displayValue;
    protected int hashCode;

    public LabelImpl(String displayValue)
    {
        this.displayValue = displayValue;
        this.hashCode = displayValue.hashCode();
    }

    public LabelImpl(Label other)
    {
        this.displayValue = String.valueOf(other);
        this.hashCode = other.hashCode();
    }

    @Override
    public String toString()
    {
        return displayValue;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Label) {
            final Label other = (Label) obj;
            return displayValue.equals(String.valueOf(other));
        } else {
            return false;
        }
    }
}
