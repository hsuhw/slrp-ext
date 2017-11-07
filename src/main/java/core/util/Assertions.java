package core.util;

public final class Assertions
{
    private Assertions()
    {
    }

    public static void argumentNotNull(Object one)
    {
        if (one == null) {
            throw new IllegalArgumentException("null argument given");
        }
    }

    public static void argumentNotNull(Object one, Object two)
    {
        if (one == null || two == null) {
            throw new IllegalArgumentException("null argument given");
        }
    }

    public static void argumentNotNull(Object one, Object two, Object three)
    {
        if (one == null || two == null || three == null) {
            throw new IllegalArgumentException("null argument given");
        }
    }

    public static void argumentNotNull(Object one, Object two, Object three, Object four)
    {
        if (one == null || two == null || three == null || four == null) {
            throw new IllegalArgumentException("null argument given");
        }
    }

    public static void argumentNotNull(Object one, Object two, Object three, Object four, Object five)
    {
        if (one == null || two == null || three == null || four == null || five == null) {
            throw new IllegalArgumentException("null argument given");
        }
    }

    public static void referenceNotNull(Object one)
    {
        if (one == null) {
            throw new IllegalStateException("null reference occurred");
        }
    }

    public static void referenceNotNull(Object one, Object two)
    {
        if (one == null || two == null) {
            throw new IllegalStateException("null reference occurred");
        }
    }

    public static void referenceNotNull(Object one, Object two, Object three)
    {
        if (one == null || two == null || three == null) {
            throw new IllegalStateException("null reference occurred");
        }
    }

    public static void referenceNotNull(Object one, Object two, Object three, Object four)
    {
        if (one == null || two == null || three == null || four == null) {
            throw new IllegalStateException("null reference occurred");
        }
    }

    public static void referenceNotNull(Object one, Object two, Object three, Object four, Object five)
    {
        if (one == null || two == null || three == null || four == null || five == null) {
            throw new IllegalStateException("null reference occurred");
        }
    }
}
