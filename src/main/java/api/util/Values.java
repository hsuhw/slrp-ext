package api.util;

public final class Values
{
    public static final String NOT_IMPLEMENTED_YET = "not implemented yet";
    public static final String NO_MATCHED_IMPLEMENTATION_FOUND = "no matched implementation found";
    public static final String DISPLAY_EPSILON_SYMBOL = "(epsilon)";
    public static final String DISPLAY_NEWLINE = System.getProperty("line.separator");
    public static final String DISPLAY_INDENT = "  ";

    private Values()
    {
    }

    public enum Direction
    {
        FORWARD, BACKWARD
    }
}
