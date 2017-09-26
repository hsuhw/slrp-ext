package api.automata;

import core.automata.BiMapAlphabetTranslator;
import core.automata.BiMapAlphabetTranslatorBuilder;
import org.eclipse.collections.api.bimap.MutableBiMap;

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

    public static <O, T> AlphabetTranslator<O, T> create(MutableBiMap<O, T> definition, O originEpsilonSymbol)
    {
        return new BiMapAlphabetTranslator<>(definition, originEpsilonSymbol);
    }
}
