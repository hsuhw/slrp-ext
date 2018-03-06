package core.automata.fst;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.MutableAutomaton;
import api.automata.MutableState;
import api.automata.fst.MutableFST;
import core.automata.MapSetState;
import core.automata.fsa.BasicMutableFSA;
import org.eclipse.collections.api.tuple.Pair;

public class BasicMutableFST<S, T> extends AbstractMutableFST<S, T> implements MutableFST<S, T>
{
    public BasicMutableFST(Alphabet<Pair<S, T>> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);
    }

    public BasicMutableFST(BasicMutableFST<S, T> toCopy, boolean deep)
    {
        super(toCopy, deep);
    }

    @Override
    public <U, R> Automaton<R> product(Automaton<U> target, Alphabet<R> alphabet, StepMaker<Pair<S, T>, U, R> stepMaker,
        Finalizer<Pair<S, T>, U, R> finalizer)
    {
        final int capacity = states().size() * target.states().size(); // upper bound
        if (alphabet.epsilon() instanceof Pair<?, ?>) {
            @SuppressWarnings("unchecked")
            final MutableAutomaton<R> result = new BasicMutableFST(alphabet, capacity);
            return (new ProductHandler<>(target, result, capacity)).makeProduct(stepMaker).settle(finalizer);
        } else {
            final MutableAutomaton<R> result = new BasicMutableFSA<>(alphabet, capacity);
            return (new ProductHandler<>(target, result, capacity)).makeProduct(stepMaker).settle(finalizer);
        }
    }

    @Override
    protected MutableState<Pair<S, T>> createState()
    {
        return new MapSetState<>(alphabet.size());
    }

    @Override
    public String toString()
    {
        return toString("", "");
    }
}
