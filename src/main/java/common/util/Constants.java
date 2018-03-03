package common.util;

public final class Constants
{
    public static final String NOT_IMPLEMENTED_YET = "not implemented yet";
    public static final String NO_IMPLEMENTATION_FOUND = "no matched implementation found";
    public static final String DISPLAY_NEWLINE = System.getProperty("line.separator");
    public static final String DISPLAY_INDENT = "  ";

    public enum Direction
    {
        FORWARD, BACKWARD
    }

    private Constants()
    {
    }
}
