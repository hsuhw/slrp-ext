package api.automata.fst;

import api.automata.Alphabet;
import api.automata.MutableAutomaton;
import api.automata.MutableState;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import core.automata.AbstractMutableAutomaton;
import core.automata.fst.BasicMutableFST;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.LinkedList;
import java.util.List;

import static common.util.Constants.NO_IMPLEMENTATION_FOUND;

public final class FSTs
{
    private FSTs()
    {
    }

    public static <S, T> MutableFST<S, T> create(Alphabet<Pair<S, T>> alphabet, int stateCapacity)
    {
        return new BasicMutableFST<>(alphabet, stateCapacity);
    }

    public static <S, T> MutableFST<S, T> shallowCopy(MutableFST<S, T> target)
    {
        if (target instanceof BasicMutableFST<?, ?>) {
            return new BasicMutableFST<>((BasicMutableFST<S, T>) target, false);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    public static <S, T> MutableFST<S, T> deepCopy(MutableFST<S, T> target)
    {
        if (target instanceof BasicMutableFST<?, ?>) {
            return new BasicMutableFST<>((BasicMutableFST<S, T>) target, true);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }

    public static <S, T> FST<S, T> castFrom(MutableAutomaton<Pair<S, T>> target)
    {
        if (target instanceof AbstractMutableAutomaton<?>) {
            return new BasicMutableFST<>((AbstractMutableAutomaton<Pair<S, T>>) target);
        }

        throw new UnsupportedOperationException(NO_IMPLEMENTATION_FOUND);
    }
}
