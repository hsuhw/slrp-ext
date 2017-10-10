package core.automata;

import api.automata.Alphabet;
import api.automata.AlphabetEncoder;
import api.automata.Alphabets;
import core.util.Assertions;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;

public class BiMapAlphabetEncoder<S, T> implements AlphabetEncoder<S, T>
{
    private final ImmutableBiMap<S, T> encoder;
    private final ImmutableBiMap<T, S> decoder;
    private final S originEpsilon;

    public BiMapAlphabetEncoder(MutableBiMap<S, T> definition, S originEpsilon)
    {
        Assertions.argumentNotNull(originEpsilon);
        if (!definition.containsKey(originEpsilon)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.containsKey(null) || definition.containsValue(null)) {
            throw new IllegalArgumentException("a null reference found in the definition");
        }

        final ImmutableBiMap<S, T> symbolTable = (ImmutableBiMap<S, T>) definition.toImmutable();
        encoder = symbolTable;
        decoder = symbolTable.inverse();
        this.originEpsilon = originEpsilon;
    }

    @Override
    public int size()
    {
        return encoder.size();
    }

    @Override
    public T encodedEpsilon()
    {
        return encoder.get(originEpsilon);
    }

    @Override
    public Alphabet<T> encodedAlphabet()
    {
        return Alphabets.create(encoder.valuesView().toSet(), encodedEpsilon());
    }

    @Override
    public S originEpsilon()
    {
        return originEpsilon;
    }

    @Override
    public Alphabet<S> originAlphabet()
    {
        return Alphabets.create(encoder.keysView().toSet(), originEpsilon());
    }

    @Override
    public T encode(S symbol)
    {
        return encoder.get(symbol);
    }

    @Override
    public S decode(T symbol)
    {
        return decoder.get(symbol);
    }
}
