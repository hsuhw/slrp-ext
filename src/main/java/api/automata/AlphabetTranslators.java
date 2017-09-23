package api.automata;

import core.automata.BiMapAlphabetTranslatorBuilder;

import static api.automata.AlphabetTranslator.Builder;

public final class AlphabetTranslators
{
    private AlphabetTranslators()
    {
    }

    public static <O, T> Builder<O, T> builder(int symbolNumberEstimate)
    {
        return new BiMapAlphabetTranslatorBuilder<>(symbolNumberEstimate);
    }
}
