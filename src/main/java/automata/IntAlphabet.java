package automata;

import org.eclipse.collections.api.map.primitive.ImmutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

public class IntAlphabet implements Alphabet
{
    public static final int EPSILON = 0;

    private final MutableObjectIntMap<String> instance;

    public IntAlphabet(String epsilon)
    {
        instance = new ObjectIntHashMap<>();
        instance.put(epsilon, EPSILON);
    }

    public ImmutableObjectIntMap<String> getInstance()
    {
        return instance.toImmutable();
    }

    @Override
    public int size()
    {
        return instance.size();
    }

    public int getIfAbsentPut(String symbol)
    {
        return instance.getIfAbsentPut(symbol, instance.size());
    }
}
