package core.parser;

import api.automata.*;
import common.util.Assert;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.List;

import static api.parser.Parser.SymbolPolicy;
import static api.parser.Parser.SymbolPolicy.AGGREGATE;
import static api.parser.Parser.SymbolPolicy.SEPARATE;
import static core.Parameters.PARSER_COMMON_CAPACITY;

public abstract class AbstractAutomatonListListener<S>
{
    private final MutableList<MutableAutomaton<S>> result;
    private final SymbolPolicy symbolPolicy;
    private final S epsilonSymbol;
    private Alphabet<S> predefinedAlphabet;
    private Alphabet.Builder<S> currAlphabet;
    private MutableMap<String, MutableState<S>> stateNameTable;
    private MutableAutomaton<S> currBuilder;

    public AbstractAutomatonListListener(Alphabet<S> baseAlphabet, SymbolPolicy policy)
    {
        Assert.argumentNotNull(baseAlphabet);

        result = FastList.newList(PARSER_COMMON_CAPACITY);
        symbolPolicy = policy;
        epsilonSymbol = baseAlphabet.epsilon();
        predefinedAlphabet = baseAlphabet;
        if (symbolPolicy == AGGREGATE) {
            currAlphabet = Alphabets.builder(baseAlphabet);
        }
    }

    public AbstractAutomatonListListener(S epsilonSymbol, SymbolPolicy policy)
    {
        switch (policy) {
            case PREDEFINED:
                throw new IllegalArgumentException("no predefined alphabet specified");
            case AGGREGATE:
                currAlphabet = Alphabets.builder(PARSER_COMMON_CAPACITY, epsilonSymbol);
                break;
            case SEPARATE:
                predefinedAlphabet = Alphabets.create(UnifiedSet.newSet(PARSER_COMMON_CAPACITY), epsilonSymbol);
                break;
            default:
                break; // should not happen
        }
        result = FastList.newList(PARSER_COMMON_CAPACITY);
        symbolPolicy = policy;
        this.epsilonSymbol = epsilonSymbol;
    }

    public AbstractAutomatonListListener(Alphabet.Builder<S> alphabetBuilder)
    {
        Assert.argumentNotNull(alphabetBuilder);

        result = FastList.newList(PARSER_COMMON_CAPACITY);
        symbolPolicy = AGGREGATE;
        epsilonSymbol = alphabetBuilder.epsilon();
        currAlphabet = alphabetBuilder;
    }

    public ListIterable<? extends Automaton<S>> result()
    {
        if (symbolPolicy == AGGREGATE) {
            final var alphabet = currAlphabet.build();
            result.forEach(automaton -> automaton.setAlphabet(alphabet));
        }

        return result;
    }

    protected abstract MutableAutomaton<S> newBuilder(Alphabet<S> dummyAlphabet, int stateCapacity);

    public void enterAutomaton(int startLineNo, int endLineNo)
    {
        final var capacity = endLineNo - startLineNo + 1; // heuristic
        if (symbolPolicy == SEPARATE) {
            currAlphabet = Alphabets.builder(predefinedAlphabet);
        }
        stateNameTable = UnifiedMap.newMap(capacity);
        currBuilder = newBuilder(new DummyAlphabet(), capacity);
    }

    public void exitAutomaton()
    {
        switch (symbolPolicy) {
            case PREDEFINED:
                result.add(currBuilder.setAlphabet(predefinedAlphabet));
            case AGGREGATE:
                result.add(currBuilder);
                break;
            case SEPARATE:
                result.add(currBuilder.setAlphabet(currAlphabet.build()));
                currAlphabet = null;
                break;
            default:
                break; // should not happen
        }
        stateNameTable = null;
        currBuilder = null;
    }

    private MutableState<S> takeState(String name)
    {
        return stateNameTable.computeIfAbsent(name, __ -> currBuilder.newState(name));
    }

    public void enterStartStates(List<TerminalNode> stateNodes)
    {
        final var startState = currBuilder.startState();
        if (stateNodes.size() == 1 && stateNodes.get(0) != null) {
            currBuilder.setAsStart(takeState(stateNodes.get(0).getText()));
            currBuilder.removeState(startState);
        } else {
            stateNodes.forEach(id -> currBuilder.addEpsilonTransition(startState, takeState(id.getText())));
        }
    }

    public void enterTransition(List<TerminalNode> stateNodes, S symbol)
    {
        final var dept = takeState(stateNodes.get(0).getText());
        final var dest = takeState(stateNodes.get(1).getText());
        currAlphabet.add(symbol);
        currBuilder.addTransition(dept, dest, symbol);
    }

    public void enterAcceptStates(List<TerminalNode> stateNodes)
    {
        stateNodes.forEach(id -> currBuilder.setAsAccept(takeState(id.getText())));
    }

    private class DummyAlphabet implements Alphabet<S>
    {
        @Override
        public int size()
        {
            return 0;
        }

        @Override
        public S epsilon()
        {
            return epsilonSymbol;
        }

        @Override
        public SetIterable<S> asSet()
        {
            return null;
        }

        @Override
        public SetIterable<S> noEpsilonSet()
        {
            return null;
        }

        @Override
        public boolean contains(S symbol)
        {
            return true; // help to bypass the `addTransition` symbol check of `currBuilder`
        }
    }
}
