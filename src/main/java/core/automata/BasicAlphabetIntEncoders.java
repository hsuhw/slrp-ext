package core.automata;

import api.automata.Alphabet;
import api.automata.AlphabetIntEncoder;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

public final class BasicAlphabetIntEncoders implements AlphabetIntEncoder.Provider
{
    public <S> AlphabetIntEncoder<S> create(MutableList<S> definition, S epsilon)
    {
        return new MapListAlphabetIntEncoder<>(definition, epsilon);
    }

    public <S> AlphabetIntEncoder<S> create(Alphabet<S> alphabet)
    {
        final MutableList<S> definition = FastList.newList(alphabet.size());
        definition.add(alphabet.epsilon());
        definition.addAllIterable(alphabet.noEpsilonSet());

        return create(definition, alphabet.epsilon());
    }
}
