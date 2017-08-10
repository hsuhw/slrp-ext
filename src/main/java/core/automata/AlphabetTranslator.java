package core.automata;

import api.automata.Alphabet;
import api.automata.Symbol;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.set.ImmutableSet;

public class AlphabetTranslator<O, T extends Symbol> implements api.automata.AlphabetTranslator<O, T>
{
    private final ImmutableBiMap<O, T> encoder;
    private final ImmutableBiMap<T, O> decoder;
    private final O originEpsilonSymbol;

    public AlphabetTranslator(ImmutableBiMap<O, T> definition, O epsilonSymbol)
    {
        if (!definition.containsKey(epsilonSymbol)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        encoder = definition;
        decoder = definition.inverse();
        originEpsilonSymbol = epsilonSymbol;
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
        return new core.automata.Alphabet<>(encoder.valuesView().toSet().toImmutable(), getTargetEpsilonSymbol());
    }

    @Override
    public ImmutableSet<O> getOriginAlphabet()
    {
        return encoder.keysView().toSet().toImmutable();
    }
}
