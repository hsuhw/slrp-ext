package core.automata;

import api.automata.AlphabetEncoder;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;

import static api.automata.AlphabetEncoder.Builder;

public class BiMapAlphabetEncoderBuilder<S, T> implements AlphabetEncoder.Builder<S, T>
{
    private final MutableBiMap<S, T> symbolTable;
    private S originEpsilon;

    public BiMapAlphabetEncoderBuilder(int sizeEstimate)
    {
        symbolTable = new HashBiMap<>(sizeEstimate);
    }

    @Override
    public Builder<S, T> define(S origin, T target)
    {
        symbolTable.put(origin, target);

        return this;
    }

    @Override
    public Builder<S, T> defineEpsilon(S origin, T target)
    {
        if (originEpsilon != null) {
            symbolTable.remove(originEpsilon);
        }
        originEpsilon = origin;
        symbolTable.put(origin, target);

        return this;
    }

    @Override
    public AlphabetEncoder<S, T> build()
    {
        return new BiMapAlphabetEncoder<>(symbolTable, originEpsilon);
    }
}
