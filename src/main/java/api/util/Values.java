package api.util;

import org.apache.commons.text.RandomStringGenerator;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

public final class Values
{
    public static final String NOT_IMPLEMENTED_YET = "not implemented yet";
    public static final String NO_MATCHED_IMPLEMENTATION_FOUND = "no matched implementation found";
    public static final String EPSILON_SYMBOL_DISPLAY_VALUE = "(epsilon)";

    private Values()
    {
    }

    private static final RandomStringGenerator lowerCasedAlphanumericRSG = new RandomStringGenerator.Builder()
        .withinRange('0', 'z').filteredBy(c -> (isLetter(c) && isLowerCase(c)), DIGITS).build();
    private static final RandomStringGenerator alphanumericRSG = new RandomStringGenerator.Builder()
        .withinRange('0', 'z').filteredBy(LETTERS, DIGITS).build();

    public static String randomAlphanumeric(int length, boolean includesUpperCase)
    {
        return includesUpperCase ? alphanumericRSG.generate(length) : lowerCasedAlphanumericRSG.generate(length);
    }
}
