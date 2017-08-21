package util;

import org.apache.commons.text.RandomStringGenerator;

import java.lang.reflect.Array;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

public final class Misc
{
    public static final String NIY = "not implemented yet";
    public static final String NMI = "no matched implementation found";
    public static final String EPSILON_SYMBOL_DISPLAY_VALUE = "(epsilon)";

    private Misc()
    {
    }

    public static <T> T[] newArrayForLike(T example, int size)
    {
        @SuppressWarnings("unchecked")
        final T[] array = (T[]) Array.newInstance(example.getClass(), size);
        return array;
    }

    private static final RandomStringGenerator lowerCasedAlphanumericRSG = new RandomStringGenerator.Builder()
        .withinRange('0', 'z').filteredBy(LETTERS, DIGITS, Character::isLowerCase).build();
    private static final RandomStringGenerator alphanumericRSG = new RandomStringGenerator.Builder()
        .withinRange('0', 'z').filteredBy(LETTERS, DIGITS).build();

    public static String randomAlphanumeric(int length, boolean includesUpperCase)
    {
        return includesUpperCase ? alphanumericRSG.generate(length) : lowerCasedAlphanumericRSG.generate(length);
    }
}
