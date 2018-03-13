package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fsa.MutableFSA;
import core.automata.AbstractMutableAutomaton;
import core.automata.MapSetState;
import org.eclipse.collections.api.RichIterable;

public class BasicMutableFSA<S> extends AbstractMutableFSA<S> implements MutableFSA<S>
{
    private LanguageSubsetChecker languageSubsetChecker;

    public BasicMutableFSA(Alphabet<S> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);

        languageSubsetChecker = new LightLanguageSubsetChecker();
    }

    public BasicMutableFSA(BasicMutableFSA<S> toCopy, boolean deep)
    {
        super(toCopy, deep);

        languageSubsetChecker = toCopy.languageSubsetChecker;
    }

    public BasicMutableFSA(AbstractMutableAutomaton<S> toCast)
    {
        super(toCast, false);

        languageSubsetChecker = new LightLanguageSubsetChecker();
    }

    @Override
    public FSA<S> trimUnreachableStates()
    {
        final RichIterable<State<S>> unreachableStates = unreachableStates();
        if (unreachableStates.isEmpty()) {
            return this;
        }

        final BasicMutableFSA<S> result = new BasicMutableFSA<>(this, false);
        result.states.removeAllIterable(unreachableStates);

        return result;
    }

    @Override
    public <T, R> Automaton<R> product(Automaton<T> target, Alphabet<R> alphabet, StepMaker<S, T, R> stepMaker,
        Finalizer<S, T, R> finalizer)
    {
        final int capacity = states().size() * target.states().size(); // upper bound
        final MutableAutomaton<R> result = new BasicMutableFSA<>(alphabet, capacity);

        return new ProductHandler<>(target, result, capacity).makeProduct(stepMaker).settle(finalizer);
    }

    @Override
    public LanguageSubsetChecker.Result<S> checkContaining(FSA<S> target)
    {
        return languageSubsetChecker.test(target, this);
    }

    @Override
    protected MutableState<S> createState()
    {
        return new MapSetState<>(alphabet().size());
    }

    @Override
    public String toString()
    {
        return toString("", "");
    }
}
