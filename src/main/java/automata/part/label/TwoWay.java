package automata.part.label;

import automata.part.Label;
import automata.part.BasicLabel;

public final class TwoWay<L extends Label> extends BasicLabel implements Label
{
    private final L label;
    private final HeadMove headMove;

    public enum HeadMove
    {
        LEFT("L"), STAY("S"), RIGHT("R");

        private final String displayValue;

        HeadMove(String displayValue)
        {
            this.displayValue = displayValue;
        }

        @Override
        public String toString()
        {
            return this.displayValue;
        }
    }

    public TwoWay(L label, HeadMove headMove)
    {
        super(label);
        this.label = label;
        this.headMove = headMove;
        this.hashCode = computeHashCode();
    }

    private int computeHashCode()
    {
        int result = 54013291;

        result = 56186633 * result + label.hashCode();
        result = 56186633 * result + headMove.hashCode();

        return result;
    }

    public boolean isLeftMoving()
    {
        return headMove == HeadMove.LEFT;
    }

    public boolean isRightMoving()
    {
        return headMove == HeadMove.RIGHT;
    }

    public boolean isNonoving()
    {
        return headMove == HeadMove.STAY;
    }

    public L getLabel()
    {
        return label;
    }

    public HeadMove getHeadMove()
    {
        return headMove;
    }

    @Override
    public String toString()
    {
        final String label = String.valueOf(this.label);

        return label + " " + String.valueOf(this.headMove);
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
            final TwoWay<? extends Label> other = (TwoWay<? extends Label>) obj;

            final boolean sameLabel = label.equals(other.getLabel());
            final boolean sameHeadMove = headMove.equals(other.getHeadMove());

            return sameLabel && sameHeadMove;
        } catch (ClassCastException e) {
            return false;
        }
    }
}
