package core.automata;

import api.automata.Alphabet;
import api.automata.AlphabetEncoder;
import api.automata.Alphabets;
import common.util.Assert;
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
        Assert.argumentNotNull(originEpsilon);
        if (!definition.containsKey(originEpsilon)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.containsKey(null) || definition.containsValue(null)) {
            throw new IllegalArgumentException("null found in the definition");
        }

        final var symbolTable = definition.toImmutable();
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
        final var result = encoder.get(symbol);

        if (result == null) {
            throw new IllegalStateException("symbol " + symbol + " not present");
        }

        return result;
    }

    @Override
    public S decode(T symbol)
    {
        final var result = decoder.get(symbol);

        if (result == null) {
            throw new IllegalStateException("symbol " + symbol + " not present");
        }

        return result;
    }

    @Override
    public int hashCode()
    {
        if (hashCode == -1) {
            final var prime = 41;
            var result = 1;

            result = prime * result + encoder.hashCode();
            result = prime * result + originEpsilon.hashCode();

            hashCode = result;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof BiMapAlphabetEncoder<?, ?>) {
            try {
                @SuppressWarnings("unchecked")
                final BiMapAlphabetEncoder<S, T> other = (BiMapAlphabetEncoder) obj;
                return other.encoder.equals(this.encoder) && other.originEpsilon.equals(this.originEpsilon);
            } catch (ClassCastException e) {
                return false;
            }
        }

        return false;
    }
}
