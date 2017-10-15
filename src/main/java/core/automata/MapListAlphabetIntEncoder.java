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
        return Alphabets.create(encoder.keysView().toSet(), originEpsilon());
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
}
