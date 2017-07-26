package automata.part.label;

import automata.part.Label;
import automata.part.BasicLabel;

public class IntLabel extends BasicLabel implements Label
{
    private final int symbol;

    public IntLabel(int symbol)
    {
        super(String.valueOf(symbol));
        this.symbol = symbol;
    }

    public int getSymbol()
    {
        return symbol;
    }

    public boolean match(int symbol)
    {
        return this.symbol == symbol;
    }
}
