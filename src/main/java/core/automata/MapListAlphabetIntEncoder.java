package core.automata;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import api.automata.Alphabets;
import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public class MapListAlphabetIntEncoder<S> implements AlphabetIntEncoder<S>
{
    private final ImmutableObjectIntMap<S> encoder;
    private final ImmutableList<S> decoder;
    private Alphabet<S> originAlphabet;
    private int hashCode = -1;

    public MapListAlphabetIntEncoder(MutableList<S> definition, S epsilon)
    {
        Assertions.argumentNotNull(epsilon);
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
        final ObjectIntHashMap<S> definitionInversed = new ObjectIntHashMap<>(definition.size());
        definition.forEachWithIndex(definitionInversed::put);
        encoder = definitionInversed.toImmutable();
    }

    @Override
    public int size()
    {
        return encoder.size();
    }

    @Override
    public IntSet encodedAlphabet()
    {
        return encoder.values().toSet();
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
        return encoder.get(symbol);
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
