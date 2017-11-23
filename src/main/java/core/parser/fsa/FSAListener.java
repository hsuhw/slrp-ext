package core.parser.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.States;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import core.util.Assertions;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.List;

import static api.parser.Parser.SymbolPolicy;
import static api.parser.Parser.SymbolPolicy.*;
import static core.util.Parameters.PARSER_COMMON_CAPACITY;

public class FSAListener<S>
{
    private final SymbolPolicy symbolPolicy;
    private final MutableList<FSA.Builder<S>> queuedBuilds;
    private final MutableList<FSA<S>> builtAutomata;
    private final S epsilonSymbol;
    private Alphabet<S> predefinedAlphabet;
    private MutableMap<String, State> stateNameTable;
    private Alphabet.Builder<S> alphabetBuilder;
    private FSA.Builder<S> fsaBuilder;

    public FSAListener(Alphabet<S> alphabet, SymbolPolicy policy)
    {
        Assertions.argumentNotNull(alphabet);

        symbolPolicy = policy;
        this.epsilonSymbol = alphabet.epsilon();
        predefinedAlphabet = alphabet;
        if (symbolPolicy == AGGREGATE) {
            alphabetBuilder = Alphabets.builder(alphabet);
        }
        queuedBuilds = FastList.newList(PARSER_COMMON_CAPACITY);
        builtAutomata = FastList.newList(PARSER_COMMON_CAPACITY);
    }

    public FSAListener(S epsilonSymbol, SymbolPolicy policy)
    {
        if (policy == PREDEFINED) {
            throw new IllegalArgumentException("no predefined alphabet specified");
        }

        symbolPolicy = policy;
        switch (symbolPolicy) {
            case AGGREGATE:
                alphabetBuilder = Alphabets.builder(PARSER_COMMON_CAPACITY, epsilonSymbol);
                break;
            case SEPARATE:
                predefinedAlphabet = Alphabets.create(UnifiedSet.newSet(PARSER_COMMON_CAPACITY), epsilonSymbol);
                break;
            default:
                break; // should not happen
        }
        this.epsilonSymbol = epsilonSymbol;
        queuedBuilds = FastList.newList(PARSER_COMMON_CAPACITY);
        builtAutomata = FastList.newList(PARSER_COMMON_CAPACITY);
    }

    public FSAListener(Alphabet.Builder<S> alphabetBuilder)
    {
        Assertions.argumentNotNull(alphabetBuilder);

        symbolPolicy = AGGREGATE;
        epsilonSymbol = alphabetBuilder.epsilon();
        this.alphabetBuilder = alphabetBuilder;
        queuedBuilds = FastList.newList(PARSER_COMMON_CAPACITY);
        builtAutomata = FastList.newList(PARSER_COMMON_CAPACITY);
    }

    public ListIterable<FSA<S>> getAutomata()
    {
        if (symbolPolicy == AGGREGATE) {
            final Alphabet<S> alphabet = alphabetBuilder.build();
            queuedBuilds.forEach(builder -> builtAutomata.add(builder.buildWith(alphabet)));
        }

        return builtAutomata;
    }

    public ListIterable<FSA<S>> getAutomataWith(Alphabet<S> override)
    {
        queuedBuilds.forEach(builder -> builtAutomata.add(builder.buildWith(override)));

        return builtAutomata;
    }

    public void enterAutomaton(int startLineNo, int endLineNo)
    {
        final int capacity = endLineNo - startLineNo + 1; // heuristic
        if (symbolPolicy == SEPARATE) {
            alphabetBuilder = Alphabets.builder(predefinedAlphabet);
        }
        stateNameTable = UnifiedMap.newMap(capacity);
        fsaBuilder = FSAs.builder(capacity, capacity, epsilonSymbol);
    }

    public void exitAutomaton()
    {
        switch (symbolPolicy) {
            case PREDEFINED:
                builtAutomata.add(fsaBuilder.buildWith(predefinedAlphabet));
            case AGGREGATE:
                queuedBuilds.add(fsaBuilder);
                break;
            case SEPARATE:
                builtAutomata.add(fsaBuilder.buildWith(alphabetBuilder.build()));
                alphabetBuilder = null;
                break;
            default:
                break; // should not happen
        }
        stateNameTable = null;
        fsaBuilder = null;
    }

    private State getState(String name)
    {
        return stateNameTable.computeIfAbsent(name, __ -> States.create(name));
    }

    public void enterStartStates(List<TerminalNode> stateNodes)
    {
        stateNodes.forEach(id -> fsaBuilder.addStartState(getState(id.getText())));
    }

    public void enterTransition(List<TerminalNode> stateNodes, S symbol)
    {
        final State dept = getState(stateNodes.get(0).getText());
        final State dest = getState(stateNodes.get(1).getText());
        alphabetBuilder.add(symbol);
        fsaBuilder.addTransition(dept, dest, symbol);
    }

    public void enterAcceptStates(List<TerminalNode> stateNodes)
    {
        stateNodes.forEach(id -> fsaBuilder.addAcceptState(getState(id.getText())));
    }
}
