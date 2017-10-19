package core.automata;

import api.automata.AlphabetEncoder;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;

import static api.automata.AlphabetEncoder.Builder;
import static core.util.Parameters.estimateExtendedSize;

public class BiMapAlphabetEncoderBuilder<S, T> implements AlphabetEncoder.Builder<S, T>
{
    private final MutableBiMap<S, T> symbolTable;
    private final S originEpsilon;

    public BiMapAlphabetEncoderBuilder(int sizeEstimate, S originEpsilon, T targetEpsilon)
    {
        symbolTable = new HashBiMap<>(sizeEstimate);
        symbolTable.put(originEpsilon, targetEpsilon);
        this.originEpsilon = originEpsilon;
    }

    public BiMapAlphabetEncoderBuilder(BiMapAlphabetEncoder<S, T> alphabetEncoder)
    {
        symbolTable = new HashBiMap<>(estimateExtendedSize(alphabetEncoder.size()));
        symbolTable.putAll(alphabetEncoder.biMap().castToMap());
        originEpsilon = alphabetEncoder.originEpsilon();
    }

    @Override
    public T encodedEpsilon()
    {
        return symbolTable.get(originEpsilon);
    }

    @Override
    public S originEpsilon()
    {
        return originEpsilon;
    }

    @Override
    public Builder<S, T> define(S origin, T target)
    {
        symbolTable.put(origin, target);

        return this;
    }

    @Override
    public BiMapAlphabetEncoder<S, T> build()
    {
        return new BiMapAlphabetEncoder<>(symbolTable, originEpsilon);
    }
}
