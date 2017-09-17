package core.automata.fsa;

import api.automata.State;
import api.automata.Symbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAManipulatorDecorator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;

import java.util.Queue;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractBasicFSAManipulator implements FSAManipulatorDecorator
{
    private final FSAManipulator decoratee;

    protected AbstractBasicFSAManipulator(FSAManipulator decoratee)
    {
        this.decoratee = decoratee;
    }

    @Override
    public FSAManipulator getDecoratee()
    {
        return decoratee;
    }

    protected void prepareStateReachabilitySearch(ImmutableList<State> states, Predicate<State> initialCondition,
                                                  MutableSet<State> reachableStates, Queue<State> pendingChecks)
    {
        states.forEach(state -> {
            if (initialCondition.test(state)) {
                reachableStates.add(state);
                pendingChecks.add(state);
            }
        });
    }

    protected void computeStateReachability(Function<State, ImmutableSet<State>> stepFunction,
                                            MutableSet<State> reachableStates, Queue<State> pendingChecks)
    {
        State currState;
        while ((currState = pendingChecks.poll()) != null) {
            stepFunction.apply(currState).forEach(state -> {
                if (!reachableStates.contains(state)) {
                    reachableStates.add(state);
                    pendingChecks.add(state);
                }
            });
        }
    }

    protected <S extends Symbol> StateAttributes decideStateAttributes(FSA<S> target, MutableMap<State, ?> delta)
    {
        final MutableList<State> states = delta.keysView().toList();
        final int reachableNumber = states.size();
        final MutableBooleanList startStateTable = BooleanLists.mutable.of(new boolean[reachableNumber]);
        final MutableBooleanList acceptStateTable = BooleanLists.mutable.of(new boolean[reachableNumber]);
        states.forEachWithIndex((state, index) -> {
            if (target.isStartState(state)) {
                startStateTable.set(index, true);
            }
            if (target.isAcceptState(state)) {
                acceptStateTable.set(index, true);
            }
        });
        return new BasicFSAStateAttributes(states, startStateTable, acceptStateTable);
    }
}
