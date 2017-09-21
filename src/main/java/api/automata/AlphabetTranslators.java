package api.automata;

import core.automata.BiMapAlphabetTranslatorBuilder;

public final class AlphabetTranslators
{
    private AlphabetTranslators()
    {
    }

    public static <O, T> AlphabetTranslator.Builder<O, T> builder(int symbolNumberEstimate)
    {
        return new BiMapAlphabetTranslatorBuilder<>(symbolNumberEstimate);
    }
}
