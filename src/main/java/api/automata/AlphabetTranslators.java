package core.automata;

import api.automata.AlphabetTranslator;
import api.automata.IntAlphabetTranslator;
import api.automata.Symbol;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.BiMaps;

public final class AlphabetTranslators
{
    private AlphabetTranslators()
    {
    }

    public static <O, T extends Symbol> AlphabetTranslator<O, T> newOne(ImmutableBiMap<O, T> definition,
                                                                        O epsilonSymbol)
    {
        return new BiMapAlphabetTranslator<>(definition, epsilonSymbol);
    }

    public static <O, T extends Symbol> AlphabetTranslator<O, T> newOne(MutableBiMap<O, T> definition, O epsilonSymbol)
    {
        return new BiMapAlphabetTranslator<>(BiMaps.immutable.ofAll(definition), epsilonSymbol);
    }

    public static <S> IntAlphabetTranslator<S> createIntOne(ImmutableList<S> definition, S epsilonSymbol)
    {
        return new MapListIntAlphabetTranslator<>(definition, epsilonSymbol);
    }

    public static <S> IntAlphabetTranslator<S> createIntOne(MutableList<S> definition, S epsilonSymbol)
    {
        return new MapListIntAlphabetTranslator<>(definition.toImmutable(), epsilonSymbol);
    }
}
