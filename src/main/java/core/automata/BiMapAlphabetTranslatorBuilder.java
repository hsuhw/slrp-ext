package core.automata;

import api.automata.AlphabetTranslator;
import core.util.Assertions;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;

public final class BiMapAlphabetTranslatorBuilder<O, T> implements AlphabetTranslator.Builder<O, T>
{
    private final MutableBiMap<O, T> symbolTable;
    private O originEpsilonSymbol;

    public BiMapAlphabetTranslatorBuilder(int symbolNumberEstimate)
    {
        symbolTable = new HashBiMap<>(symbolNumberEstimate);
    }

    @Override
    public AlphabetTranslator.Builder<O, T> define(O origin, T target)
    {
        Assertions.argumentNotNull(origin, target);

        symbolTable.put(origin, target);

        return this;
    }

    @Override
    public AlphabetTranslator.Builder<O, T> defineEpsilon(O origin, T target)
    {
        Assertions.argumentNotNull(origin, target);

        if (originEpsilonSymbol != null) {
            symbolTable.remove(originEpsilonSymbol);
        }
        originEpsilonSymbol = origin;
        symbolTable.put(origin, target);

        return this;
    }

    @Override
    public AlphabetTranslator<O, T> build()
    {
        return new BiMapAlphabetTranslator<>(symbolTable, originEpsilonSymbol);
    }
}
