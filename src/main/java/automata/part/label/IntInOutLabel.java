package automata.part.label;

import automata.part.Label;
import automata.part.LabelImpl;

public class IntInOutLabel extends LabelImpl implements Label
{
    private final int in;
    private final int out;

    public IntInOutLabel(int in, int out)
    {
        super(in + "/" + out);
        this.in = in;
        this.out = out;
    }

    public int getInSymbol()
    {
        return in;
    }

    public int getOutSymbol()
    {
        return out;
    }

    public boolean match(int in)
    {
        return this.in == in;
    }
}
