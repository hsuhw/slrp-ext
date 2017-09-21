package core.automata;

import api.automata.Alphabet;
import api.automata.AlphabetTranslator;
import api.automata.Alphabets;
import core.util.Assertions;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.map.ImmutableMapIterable;

public class BiMapAlphabetTranslator<O, T> implements AlphabetTranslator<O, T>
{
    private final ImmutableMapIterable<O, T> encoder;
    private final ImmutableMapIterable<T, O> decoder;
    private final O originEpsilonSymbol;

    public BiMapAlphabetTranslator(MutableBiMap<O, T> definition, O originEpsilonSymbol)
    {
        Assertions.argumentNotNull(originEpsilonSymbol);
        if (!definition.containsKey(originEpsilonSymbol)) {
            throw new IllegalStateException("epsilon symbol not found in the definition");
        }

        final ImmutableBiMap<O, T> symbolTable = (ImmutableBiMap<O, T>) definition.toImmutable();
        encoder = symbolTable;
        decoder = symbolTable.inverse();
        this.originEpsilonSymbol = originEpsilonSymbol;
    }

    @Override
    public int size()
    {
        return encoder.size();
    }

    @Override
    public T getTargetEpsilonSymbol()
    {
        return encoder.get(originEpsilonSymbol);
    }

    @Override
    public O getOriginEpsilonSymbol()
    {
        return originEpsilonSymbol;
    }

    @Override
    public T targetSymbolOf(O symbol)
    {
        return encoder.get(symbol);
    }

    @Override
    public O originSymbolOf(T symbol)
    {
        return decoder.get(symbol);
    }

    @Override
    public Alphabet<T> getTargetAlphabet()
    {
        return Alphabets.newOne(encoder.valuesView().toSet(), getTargetEpsilonSymbol());
    }

    @Override
    public Alphabet<O> getOriginAlphabet()
    {
        return Alphabets.newOne(encoder.keysView().toSet(), getOriginEpsilonSymbol());
    }
}
