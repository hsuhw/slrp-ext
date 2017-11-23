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
    private Alphabet<T> encodedAlphabet;
    private Alphabet<S> originAlphabet;
    private int hashCode = -1;

    public BiMapAlphabetEncoder(MutableBiMap<S, T> definition, S originEpsilon)
    {
        Assertions.argumentNotNull(originEpsilon);
        if (!definition.containsKey(originEpsilon)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.containsKey(null) || definition.containsValue(null)) {
            throw new IllegalArgumentException("null found in the definition");
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
        if (encodedAlphabet == null) {
            encodedAlphabet = Alphabets.create(encoder.valuesView().toSet(), encodedEpsilon());
        }

        return encodedAlphabet;
    }

    @Override
    public S originEpsilon()
    {
        return originEpsilon;
    }

    @Override
    public Alphabet<S> originAlphabet()
    {
        if (originAlphabet == null) {
            originAlphabet = Alphabets.create(encoder.keysView().toSet(), originEpsilon());
        }

        return originAlphabet;
    }

    @Override
    public T encode(S symbol)
    {
        if (!encoder.containsKey(symbol)) {
            throw new IllegalStateException("symbol " + symbol + " not present");
        }

        return encoder.get(symbol);
    }

    @Override
    public S decode(T symbol)
    {
        if (!encoder.containsKey(symbol)) {
            throw new IllegalStateException("symbol " + symbol + " not present");
        }

        return decoder.get(symbol);
    }

    @Override
    public int hashCode()
    {
        if (hashCode == -1) {
            final int prime = 41;
            int result = 1;

            result = prime * result + encoder.hashCode();
            result = prime * result + originEpsilon.hashCode();

            hashCode = result;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof BiMapAlphabetEncoder<?, ?>) {
            try {
                @SuppressWarnings("unchecked")
                final BiMapAlphabetEncoder<S, T> other = (BiMapAlphabetEncoder<S, T>) obj;
                return other.encoder.equals(this.encoder) && other.originEpsilon.equals(this.originEpsilon);
            } catch (ClassCastException e) {
                return false;
            }
        }
        return false;
    }

    final ImmutableBiMap<S, T> biMap()
    {
        return encoder;
    }
}
