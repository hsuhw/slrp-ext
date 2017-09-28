package core.automata;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.IntAlphabetTranslator;
import core.util.Assertions;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public class MapListIntAlphabetTranslator<S> implements IntAlphabetTranslator<S>
{
    private final ImmutableObjectIntMap<S> encoder;
    private final ImmutableList<S> decoder;

    public MapListIntAlphabetTranslator(MutableList<S> definition, S epsilonSymbol)
    {
        Assertions.argumentNotNull(epsilonSymbol);
        if (!definition.contains(epsilonSymbol)) {
            throw new IllegalArgumentException("epsilon symbol not found in the definition");
        }
        if (definition.get(INT_EPSILON) != epsilonSymbol) {
            throw new IllegalArgumentException("epsilon symbol should be mapped to zero");
        }
        if (definition.contains(null)) {
            throw new IllegalArgumentException("a null reference found in the definition");
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
    public int intSymbolOf(S symbol)
    {
        return encoder.get(symbol);
    }

    @Override
    public S originSymbolOf(int symbol)
    {
        return decoder.get(symbol);
    }

    @Override
    public IntSet getIntAlphabet()
    {
        return encoder.values().toSet();
    }

    @Override
    public Alphabet<S> getOriginAlphabet()
    {
        return Alphabets.create(encoder.keysView().toSet(), getOriginEpsilonSymbol());
    }
}
