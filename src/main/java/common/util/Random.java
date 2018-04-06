package common.util;

import org.apache.commons.text.RandomStringGenerator;

import static java.lang.Character.isLetter;
import static java.lang.Character.isLowerCase;
import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

public final class Random
{
    private static final RandomStringGenerator lowerCasedAlphanumericRSG = new RandomStringGenerator.Builder()
        .withinRange('0', 'z').filteredBy(c -> (isLetter(c) && isLowerCase(c)), DIGITS).build();
    private static final RandomStringGenerator alphanumericRSG = new RandomStringGenerator.Builder()
        .withinRange('0', 'z').filteredBy(LETTERS, DIGITS).build();

    private Random()
    {
    }

    public static String alphanumeric(int length, boolean includeUpperCase)
    {
        return includeUpperCase ? alphanumericRSG.generate(length) : lowerCasedAlphanumericRSG.generate(length);
    }
}
