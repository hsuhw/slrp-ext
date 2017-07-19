package automata.part.label;

import automata.part.Label;
import automata.part.LabelImpl;

public class IntPairLabel extends LabelImpl implements Label
{
    private final int fst;
    private final int snd;

    public IntPairLabel(int fst, int snd)
    {
        super(fst + "," + snd);
        this.fst = fst;
        this.snd = snd;
    }

    public int getFstSymbol()
    {
        return fst;
    }

    public int getSndSymbol()
    {
        return snd;
    }

    public boolean match(int fst, int snd)
    {
        return this.fst == fst && this.snd == snd;
    }
}
