package api.automata;

import core.automata.MapListIntAlphabetTranslator;
import org.eclipse.collections.api.list.MutableList;

public final class IntAlphabetTranslators
{
    private IntAlphabetTranslators()
    {
    }

    public static <S> IntAlphabetTranslator<S> builder(MutableList<S> definition, S epsilonSymbol)
    {
        return new MapListIntAlphabetTranslator<>(definition, epsilonSymbol);
    }
}
