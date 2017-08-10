package core.automata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public class IntAlphabetTranslator<S> implements api.automata.IntAlphabetTranslator<S>
{
    private final ImmutableObjectIntMap<S> encoder;
    private final ImmutableList<S> decoder;

    public IntAlphabetTranslator(ImmutableList<S> definition, S epsilonSymbol)
    {
        if (!definition.contains(epsilonSymbol) || definition.get(EPSILON) != epsilonSymbol) {
            throw new IllegalArgumentException("epsilon symbol should be mapped to zero");
        }
        decoder = definition;

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
    public ImmutableIntSet getTargetAlphabet()
    {
        return encoder.values().toSet().toImmutable();
    }

    @Override
    public ImmutableSet<S> getOriginAlphabet()
    {
        return encoder.keysView().toSet().toImmutable();
    }
}
