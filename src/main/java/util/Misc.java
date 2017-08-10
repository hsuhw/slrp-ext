package util;

import java.lang.reflect.Array;

public final class Misc
{
    public static final String NIY = "not implemented yet";
    public static final String EPSILON_SYMBOL = "(epsilon)";

    public static <T> T[] newArrayForLike(T example, int size) {
        @SuppressWarnings("unchecked")
        final T[] array = (T[]) Array.newInstance(example.getClass(), size);
        return array;
    }
}
