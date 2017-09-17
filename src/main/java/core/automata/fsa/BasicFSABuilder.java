package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSABuilder;
import core.automata.Alphabets;
import core.automata.MapMapDelta;
import core.automata.MapMapSetDelta;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

public class BasicFSABuilder<S extends Symbol> implements FSABuilder<S>
{
    private static final int NONDETERMINISTIC_TRANSITION_CAPACITY = 3;

    private final int symbolNumberEstimate;
    private final S epsilonSymbol;
    private final MutableSet<S> usedSymbols;
    private final MutableList<State> states;
    private final MutableSet<State> startStates;
    private final MutableSet<State> acceptStates;
    private final MutableMap<State, MutableMap<S, MutableSet<State>>> transitionTable;

    public BasicFSABuilder(int symbolNumberEstimate, S epsilonSymbol, int stateNumberEstimate)
    {
        this.symbolNumberEstimate = symbolNumberEstimate;
        this.epsilonSymbol = epsilonSymbol;
        usedSymbols = UnifiedSet.newSet(symbolNumberEstimate);
        usedSymbols.add(epsilonSymbol);
        states = FastList.newList(stateNumberEstimate);
        startStates = UnifiedSet.newSet(stateNumberEstimate);
        acceptStates = UnifiedSet.newSet(stateNumberEstimate);
        transitionTable = UnifiedMap.newMap(stateNumberEstimate);
    }

    @Override
    public Alphabet<S> getCurrentAlphabet()
    {
        return Alphabets.createOne(usedSymbols, epsilonSymbol);
    }

    @Override
    public void addSymbol(S symbol)
    {
        usedSymbols.add(symbol);
    }

    @Override
    public void addState(State state)
    {
        if (!states.contains(state)) {
            states.add(state);
            transitionTable.put(state, UnifiedMap.newMap(symbolNumberEstimate));
        }
    }

    @Override
    public void addStartState(State state)
    {
        addState(state);
        startStates.add(state);
    }

    @Override
    public void addAcceptState(State state)
    {
        addState(state);
        acceptStates.add(state);
    }

    @Override
    public void addTransition(State dept, State dest, S symbol)
    {
        addState(dept);
        addState(dest);
        addSymbol(symbol);
        final MutableMap<S, MutableSet<State>> stateTrans = transitionTable.get(dept);

        if (!stateTrans.containsKey(symbol)) {
            stateTrans.put(symbol, UnifiedSet.newSet(NONDETERMINISTIC_TRANSITION_CAPACITY));
        }
        stateTrans.get(symbol).add(dest);
    }

    private boolean moreThanOnePossibleTrans(MutableMap<S, MutableSet<State>> stateTrans)
    {
        return stateTrans.anySatisfy(that -> that.size() > 1);
    }

    private boolean isNondeterministicTarget()
    {
        return transitionTable.anySatisfy(this::moreThanOnePossibleTrans);
    }

    private MapMapDelta<S> buildDeterministicDelta()
    {
        return new MapMapDelta<>(transitionTable.collect((dept, stateTrans) -> {
            return Tuples.pair(dept, stateTrans.collect((sym, dest) -> {
                return Tuples.pair(sym, dest.getOnly());
            }));
        }));
    }

    private FSA<S> settleRecords(Alphabet<S> alphabet)
    {
        // settle state records
        final ImmutableList<State> states = this.states.toImmutable();
        final boolean[] isStartState = new boolean[states.size()];
        final boolean[] isAcceptState = new boolean[states.size()];
        states.forEachWithIndex((s, i) -> {
            isStartState[i] = startStates.contains(s);
            isAcceptState[i] = acceptStates.contains(s);
        });
        final ImmutableBooleanList startStateTable = BooleanLists.immutable.of(isStartState);
        final ImmutableBooleanList acceptStateTable = BooleanLists.immutable.of(isAcceptState);

        // settle transition records
        if (!isNondeterministicTarget()) {
            final MapMapDelta<S> delta = buildDeterministicDelta();
            return new MapMapDFSA<>(alphabet, states, startStateTable, acceptStateTable, delta);
        } else {
            final MapMapSetDelta<S> delta = new MapMapSetDelta<>(transitionTable);
            return new MapMapSetNFSA<>(alphabet, states, startStateTable, acceptStateTable, delta);
        }
    }

    @Override
    public FSA<S> build()
    {
        return settleRecords(getCurrentAlphabet());
    }

    @Override
    public FSA<S> build(Alphabet<S> alphabet)
    {
        if (!alphabet.toSet().containsAll(usedSymbols)) {
            throw new IllegalArgumentException("given alphabet does not contain all the symbols");
        }
        return settleRecords(alphabet);
    }
}
