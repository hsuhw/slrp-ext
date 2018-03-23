package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Automaton;
import api.automata.MutableAutomaton;
import api.automata.MutableState;
import api.automata.fsa.FSA;
import api.automata.fsa.LanguageSubsetChecker;
import api.automata.fsa.MutableFSA;
import core.automata.AbstractMutableAutomaton;
import core.automata.MapSetState;

public class BasicMutableFSA<S> extends AbstractMutableFSA<S> implements MutableFSA<S>
{
    private static final LanguageSubsetChecker BASIC_LANGUAGE_SUBSET_CHECKER;
    private static final LanguageSubsetChecker LIGHT_LANGUAGE_SUBSET_CHECKER;

    static {
        BASIC_LANGUAGE_SUBSET_CHECKER = new BasicLanguageSubsetChecker();
        LIGHT_LANGUAGE_SUBSET_CHECKER = new LightLanguageSubsetChecker();
    }

    public BasicMutableFSA(Alphabet<S> alphabet, int stateCapacity)
    {
        super(alphabet, stateCapacity);
    }

    public BasicMutableFSA(BasicMutableFSA<S> toCopy, boolean deep)
    {
        super(toCopy, deep);
    }

    public BasicMutableFSA(AbstractMutableAutomaton<S> toCast)
    {
        super(toCast, false);
    }

    @Override
    public <T, R> Automaton<R> product(Automaton<T> target, Alphabet<R> alphabet, StepMaker<S, T, R> stepMaker,
        Finalizer<S, T, R> finalizer)
    {
        final var capacity = states().size() * target.states().size(); // upper bound
        final MutableAutomaton<R> result = new BasicMutableFSA<>(alphabet, capacity);

        return new ProductHandler<>(target, result, capacity).makeProduct(stepMaker).settle(finalizer);
    }

    @Override
    public LanguageSubsetChecker.Result<S> checkContaining(FSA<S> target)
    {
        return LIGHT_LANGUAGE_SUBSET_CHECKER.test(target, this);
    }

    @Override
    public LanguageSubsetChecker.Result<S> checkContainingWithCounterSource(FSA<S> target)
    {
        return BASIC_LANGUAGE_SUBSET_CHECKER.test(target, this);
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
