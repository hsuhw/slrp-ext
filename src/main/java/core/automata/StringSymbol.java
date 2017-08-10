package core.automata;

import api.automata.Symbol;

public class StringSymbol implements Symbol
{
    private final String displayValue;

    public StringSymbol(String displayValue)
    {
        this.displayValue = displayValue;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof StringSymbol) {
            final StringSymbol other = (StringSymbol) obj;
            return String.valueOf(this).equals(String.valueOf(other));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return displayValue.hashCode();
    }

    @Override
    public String toString()
    {
        return displayValue;
    }
}
