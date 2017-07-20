package parser.fsa;

import automata.IntAlphabet;
import automata.fsa.IntLabelFSA;
import automata.part.MutableState;
import automata.part.MutableStateImpl;
import automata.part.State;
import automata.part.Transition;
import automata.part.label.IntLabel;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.api.map.primitive.MutableIntBooleanMap;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntBooleanHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import parser.generated.AutomatonBasicBaseListener;
import parser.generated.AutomatonBasicParser;

import java.util.Arrays;
import java.util.List;

public class IntLabelFSABasicBuilder extends AutomatonBasicBaseListener
{
    private static final int N_TRANSITION_CAPACITY = 3;
    private final IntAlphabet alphabet;
    private List<IntLabelFSA> builtAutomata;
    private Bookkeeper keeper;

    public IntLabelFSABasicBuilder(IntAlphabet alphabet)
    {
        this.alphabet = alphabet;
        builtAutomata = new FastList<>();
    }

    public List<IntLabelFSA> getAutomata()
    {
        return builtAutomata;
    }

    private int estimateNeededContainerSpace(ParserRuleContext ctx)
    {
        final int beginLineNum = ctx.getStart().getLine();
        final int endLineNum = ctx.getStop().getLine();
        final int lineSpread = endLineNum - beginLineNum + 1;

        return lineSpread - 4;
    }

    @Override
    public void enterAutomaton(AutomatonBasicParser.AutomatonContext ctx)
    {
        keeper = new Bookkeeper(estimateNeededContainerSpace(ctx), alphabet);
    }

    @Override
    public void exitAutomaton(AutomatonBasicParser.AutomatonContext ctx)
    {
        builtAutomata.add(keeper.settleRecords());
        keeper = null;
    }

    @Override
    public void enterStartStates(AutomatonBasicParser.StartStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> keeper.takeStartState(id.getText()));
    }

    @Override
    public void enterTransition(AutomatonBasicParser.TransitionContext ctx)
    {
        final String from = ctx.ID(0).getText();
        final String to = ctx.ID(1).getText();
        final String symbol = ctx.transitionLabel().epsilonTransitionLabel() != null
                              ? ""
                              : ctx.transitionLabel().monadTransitionLabel().getText();
        keeper.takeTransition(from, to, symbol);
    }

    @Override
    public void enterAcceptStates(AutomatonBasicParser.AcceptStatesContext ctx)
    {
        ctx.stateList().ID().forEach(id -> keeper.takeAcceptState(id.getText()));
    }

    private static class Bookkeeper
    {
        private final int capacity;
        private final IntAlphabet alphabet;

        private MutableObjectIntMap<String> stateIndexTable;
        private MutableList<MutableIntObjectMap<MutableIntList>> stateTransTable;
        private MutableIntBooleanMap startStateTable;
        private MutableIntBooleanMap acceptStateTable;
        private ImmutableList<MutableState> states;
        private ImmutableList<IntLabel> labels;

        Bookkeeper(int capacity, IntAlphabet alphabet)
        {
            this.capacity = capacity;
            this.alphabet = alphabet;
            stateIndexTable = new ObjectIntHashMap<>(capacity);
            stateTransTable = new FastList<>(capacity);
            startStateTable = new IntBooleanHashMap(capacity);
            acceptStateTable = new IntBooleanHashMap(capacity);
        }

        int getSymbolIndex(String symbol)
        {
            return alphabet.getIfAbsentPut(symbol);
        }

        int getStateIndex(String stateName)
        {
            return stateIndexTable.getIfAbsentPut(stateName, () -> {
                stateTransTable.add(new IntObjectHashMap<>(capacity));
                return stateIndexTable.size();
            });
        }

        void takeStartState(String stateName)
        {
            startStateTable.put(getStateIndex(stateName), true);
        }

        void takeAcceptState(String stateName)
        {
            acceptStateTable.put(getStateIndex(stateName), true);
        }

        void takeTransition(String deptStateName, String destStateName, String symbol)
        {
            final int dept = getStateIndex(deptStateName);
            final int dest = getStateIndex(destStateName);
            final int s = getSymbolIndex(symbol);

            if (!stateTransTable.get(dept).containsKey(s)) {
                stateTransTable.get(dept).put(s, new IntArrayList(N_TRANSITION_CAPACITY));
            }
            stateTransTable.get(dept).get(s).add(dest);
        }

        private ImmutableList<IntLabel> prepareIntLabels()
        {
            final IntLabel[] labels = new IntLabel[alphabet.size()];
            alphabet.getInstance().forEachValue(s -> labels[s] = new IntLabel(s));
            return Lists.immutable.of(labels);
        }

        private ImmutableList<MutableState> prepareMutableStates()
        {
            final MutableState[] states = new MutableStateImpl[stateIndexTable.size()];
            stateIndexTable.forEachKeyValue((n, i) -> states[i] = new MutableStateImpl(i, n, i));
            return Lists.immutable.of(states);
        }

        private void finalizeDeterministicState(MutableState state, MutableIntObjectMap<MutableIntList> transitions)
        {
            final Transition[] array = new Transition[alphabet.size()];
            @SuppressWarnings("unchecked")
            final Transition<IntLabel>[] dTrans = (Transition<IntLabel>[]) array;
            transitions.forEachKeyValue((symbol, trans) -> {
                if (trans != null) { // not a dead transition on the symbol
                    final State dest = states.get(trans.getFirst());
                    dTrans[symbol] = new Transition<>(state, dest, labels.get(symbol));
                }
            });
            state.setDTransitions(Lists.immutable.of(dTrans));
        }

        private void finalizeNondeterministicState(MutableState state, MutableIntObjectMap<MutableIntList> transitions)
        {
            final ImmutableList[] array = new ImmutableList[alphabet.size()];
            @SuppressWarnings("unchecked")
            final ImmutableList<Transition<IntLabel>>[] nTrans = (ImmutableList<Transition<IntLabel>>[]) array;
            transitions.forEachKeyValue((symbol, transList) -> {
                if (transList != null) { // has some transitions on the symbol
                    final MutableList<Transition<IntLabel>> transes = transList
                        .collect(dest -> new Transition<>(state, states.get(dest), labels.get(symbol)));
                    nTrans[symbol] = transes.toImmutable();
                }
            });
            state.setNTransitions(Arrays.asList(nTrans));
        }

        IntLabelFSA settleRecords()
        {
            // since the immutability, label objects are shared
            labels = prepareIntLabels();
            states = prepareMutableStates();

            // settle state objects
            stateTransTable.forEachWithIndex((stateTrans, stateIndex) -> {
                final MutableState state = states.get(stateIndex);
                if (stateTrans.get(IntAlphabet.EPSILON) == null && stateTrans
                    .allSatisfy(each -> each == null || each.size() == 1)) {
                    // the state has deterministic transitions
                    finalizeDeterministicState(state, stateTrans);
                } else {
                    // the state has nondeterministic transitions
                    finalizeNondeterministicState(state, stateTrans);
                }
            });
            final ImmutableList<State> states = this.states.collect(s -> (State) s).toImmutable();

            // settle start state records
            final boolean[] isStartState = new boolean[states.size()];
            startStateTable.forEachKeyValue((state, yn) -> isStartState[state] = yn);
            final ImmutableBooleanList startStates = BooleanLists.immutable.of(isStartState);

            // settle accept state records
            final boolean[] isAcceptState = new boolean[states.size()];
            acceptStateTable.forEachKeyValue((state, yn) -> isAcceptState[state] = yn);
            final ImmutableBooleanList acceptStates = BooleanLists.immutable.of(isAcceptState);

            return new IntLabelFSA(states, startStates, acceptStates);
        }
    }
}
