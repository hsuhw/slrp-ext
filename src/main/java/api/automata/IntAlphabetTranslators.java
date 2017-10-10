package api.automata;

import core.automata.MapListIntAlphabetTranslator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

public final class IntAlphabetTranslators
{
    private IntAlphabetTranslators()
    {
    }

    public static <S> IntAlphabetTranslator<S> create(MutableList<S> definition, S epsilonSymbol)
    {
        return new MapListIntAlphabetTranslator<>(definition, epsilonSymbol);
    }

    public static <S> IntAlphabetTranslator<S> create(Alphabet<S> alphabet)
    {
        final MutableList<S> definition = FastList.newList(alphabet.size());
        final S epsilonSymbol = alphabet.getEpsilonSymbol();
        definition.add(epsilonSymbol);
        definition.addAllIterable(alphabet.getNoEpsilonSet());
        return create(definition, epsilonSymbol);
    }
}
