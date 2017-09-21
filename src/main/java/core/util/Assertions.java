package core.util;

public final class Assertions
{
    public static void argumentNotNull(Object one)
    {
        if (one == null) {
            throw new IllegalArgumentException("forbidden null pointer given as an argument");
        }
    }

    public static void argumentNotNull(Object one, Object two)
    {
        if (one == null || two == null) {
            throw new IllegalArgumentException("forbidden null pointer given as an argument");
        }
    }

    public static void argumentNotNull(Object one, Object two, Object three)
    {
        if (one == null || two == null || three == null) {
            throw new IllegalArgumentException("forbidden null pointer given as an argument");
        }
    }

    public static void argumentNotNull(Object one, Object two, Object three, Object four)
    {
        if (one == null || two == null || three == null || four == null) {
            throw new IllegalArgumentException("forbidden null pointer given as an argument");
        }
    }

    public static void argumentNotNull(Object one, Object two, Object three, Object four, Object five)
    {
        if (one == null || two == null || three == null || four == null || five == null) {
            throw new IllegalArgumentException("forbidden null pointer given as an argument");
        }
    }
}
