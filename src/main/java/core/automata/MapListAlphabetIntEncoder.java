package core.automata;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.Alphabets;
import common.util.Assert;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.eclipse.collections.impl.list.primitive.IntInterval;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public class MapListAlphabetIntEncoder<S> implements AlphabetIntEncoder<S>
{
    private final ImmutableObjectIntMap<S> encoder;
    private final ImmutableList<S> decoder;
    private IntSet encodedAlphabet;
    private Alphabet<S> originAlphabet;
    private int hashCode = -1;

    public MapListAlphabetIntEncoder(MutableList<S> definition, S epsilon)
    {
        Assert.argumentNotNull(epsilon);
        if (!definition.contains(epsilon)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.get(INT_EPSILON) != epsilon) {
            throw new IllegalArgumentException("epsilon symbol should be mapped to 0");
        }
        if (definition.contains(null)) {
            throw new IllegalArgumentException("null found in the definition");
        }

        decoder = definition.toImmutable();
        final ObjectIntHashMap<S> definitionInverse = new ObjectIntHashMap<>(definition.size());
        definition.forEachWithIndex(definitionInverse::put);
        encoder = definitionInverse.toImmutable();
    }

    @Override
    public int size()
    {
        return decoder.size();
    }

    @Override
    public IntSet encodedAlphabet()
    {
        if (encodedAlphabet == null) {
            encodedAlphabet = IntSets.immutable.ofAll(IntInterval.fromTo(0, size() - 1));
        }

        return encodedAlphabet;
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
    public int encode(S symbol)
    {
        return encoder.getOrThrow(symbol);
    }

    @Override
    public S decode(int symbol)
    {
        return decoder.get(symbol);
    }

    @Override
    public int hashCode()
    {
        if (hashCode == -1) {
            final int prime = 61;
            int result = 1;

            result = prime * result + decoder.hashCode();

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

        if (obj instanceof MapListAlphabetIntEncoder<?>) {
            try {
                @SuppressWarnings("unchecked")
                final MapListAlphabetIntEncoder<S> other = (MapListAlphabetIntEncoder<S>) obj;
                return other.decoder.equals(this.decoder);
            } catch (ClassCastException e) {
                return false;
            }
        }

        return false;
    }
}
