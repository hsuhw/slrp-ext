package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

import static api.util.Connectives.Labels;
import static api.util.Constants.NONEXISTING_STATE;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface MutableFSA<S extends MutableState<T>, T> extends MutableAutomaton<S, T>, FSA<S, T>
{
    @Override
    default FSA<? extends State<T>, T> trimUnreachableStates()
    {
        return FSA.upcast(FSAs.deepCopy(this).removeStates(unreachableStates()));
    }

    @Override
    default <R> FSA<? extends State<R>, R> project(Alphabet<R> alphabet, Function<T, R> projector)
    {
        final MutableFSA<MutableState<R>, R> result = FSAs.create(alphabet, states().size()); // upper bound
        final MutableMap<S, MutableState<R>> stateMapping = UnifiedMap.newMap(states().size());
        stateMapping.put(startState(), result.startState());

        R newSymbol;
        for (S dept : states()) {
            final MutableState<R> newDept = stateMapping.computeIfAbsent(dept, __ -> result.newState());
            for (T symbol : dept.enabledSymbols()) {
                for (MutableState<T> dest : dept.successors(symbol)) {
                    if ((newSymbol = projector.apply(symbol)) != null) {
                        @SuppressWarnings("unchecked")
                        final MutableState<R> newDest = stateMapping.computeIfAbsent((S) dest, __ -> result.newState());
                        result.addTransition(newDept, newDest, newSymbol);
                    }
                }
            }
        }
        acceptStates().forEach(originAccept -> result.setAsAccept(stateMapping.get(originAccept)));

        return result.trimUnreachableStates(); // one-off
    }

    @Override
    <U extends State<V>, V, R> MutableFSA<? extends MutableState<R>, R> product(Automaton<U, V> target,
        Alphabet<R> alphabet, StepMaker<S, T, U, V, R> stepMaker, Finalizer<S, U, MutableState<R>, R> finalizer);

    @Override
    default MutableFSA<S, T> toMutable()
    {
        return this;
    }

    @Override
    default MutableFSA<S, T> addSymbol(T symbol)
    {
        return (MutableFSA<S, T>) MutableAutomaton.super.addSymbol(symbol);
    }

    @Override
    MutableFSA<S, T> addState(S state);

    @Override
    default MutableFSA<S, T> addStates(RichIterable<S> states)
    {
        return (MutableFSA<S, T>) MutableAutomaton.super.addStates(states);
    }

    @Override
    MutableFSA<S, T> removeState(S state);

    @Override
    default MutableFSA<S, T> removeStates(RichIterable<S> states)
    {
        return (MutableFSA<S, T>) MutableAutomaton.super.removeStates(states);
    }

    @Override
    MutableFSA<S, T> setAsStart(S state);

    @Override
    MutableFSA<S, T> setAsAccept(S state);

    @Override
    MutableFSA<S, T> unsetAccept(S state);

    @Override
    default MutableFSA<S, T> setAllAsAccept(SetIterable<S> states)
    {
        return (MutableFSA<S, T>) MutableAutomaton.super.setAllAsAccept(states);
    }

    @Override
    MutableFSA<S, T> resetAcceptStates();

    @Override
    MutableFSA<S, T> addTransition(S dept, S dest, T symbol);

    @Override
    default MutableFSA<S, T> addEpsilonTransition(S dept, S dest)
    {
        return (MutableFSA<S, T>) MutableAutomaton.super.addEpsilonTransition(dept, dest);
    }

    @Override
    default FSA<? extends State<T>, T> determinize()
    {
        if (isDeterministic()) {
            return this;
        }

        final TransitionGraph<S, T> delta = transitionGraph();
        final int capacity = states().size() * states().size(); // heuristic
        final MutableFSA<S, T> result = FSAs.ofSameType(this, capacity);
        final MutableMap<SetIterable<S>, S> stateMapping = UnifiedMap.newMap(capacity);
        final Queue<SetIterable<S>> pendingChecks = new LinkedList<>();

        final SetIterable<S> startStates = delta.epsilonClosureOf(startState());
        stateMapping.put(startStates, result.startState());
        pendingChecks.add(startStates);
        final SetIterable<T> symbols = alphabet().noEpsilonSet();
        SetIterable<S> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            final S newDept = stateMapping.get(currStates);
            if (currStates.anySatisfy(this::isAcceptState)) {
                result.setAsAccept(newDept);
            }
            for (T symbol : symbols) {
                final SetIterable<S> destStates = delta.epsilonClosureOf(currStates, symbol);
                final S newDest = stateMapping.computeIfAbsent(destStates, __ -> {
                    pendingChecks.add(destStates);
                    return result.newState();
                });
                result.addTransition(newDept, newDest, symbol);
            }
        }

        return result; // one-off
    }

    @Override
    default FSA<? extends State<T>, T> complete()
    {
        if (!this.isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final SetIterable<S> incomplete = incompleteStates();
        final SetIterable<T> completeAlphabet = alphabet().noEpsilonSet();
        if (incomplete.isEmpty()) {
            return FSA.upcast(this);
        }

        final S sink = newState();
        completeAlphabet.forEach(symbol -> addTransition(sink, sink, symbol));
        incomplete.forEach(state -> completeAlphabet.reject(state.enabledSymbols()::contains)
                                                    .forEach(nonTrans -> addTransition(state, sink, nonTrans)));

        return FSA.upcast(this); // shallow reference
    }

    @Override
    default FSA<? extends State<T>, T> minimize()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }
        if (acceptsNone()) {
            return FSAs.acceptingNone(alphabet());
        }

        @SuppressWarnings("unchecked")
        final FSA<S, T> target = (FSA<S, T>) this.trimUnreachableStates().complete();
        final ListIterable<S> accepts = target.acceptStates().toList();
        final ListIterable<S> nonAccepts = target.nonAcceptStates().toList();
        final RichIterable<ListIterable<S>> initialPartition = Lists.immutable.of(accepts, nonAccepts);
        final ListIterable<S> initialCheck = accepts.size() < nonAccepts.size() ? accepts : nonAccepts;
        final RichIterable<ListIterable<S>> statePartition = target.refinePartition(initialPartition, initialCheck);

        final SetIterable<T> symbols = alphabet().noEpsilonSet();
        final MutableFSA<S, T> result = FSAs.ofSameType(this, statePartition.size());
        final MutableMap<ListIterable<S>, S> partitionMapping = UnifiedMap.newMap(statePartition.size());
        statePartition.forEach(part -> partitionMapping.put(part, result.newState()));
        final MutableMap<S, S> stateMapping = UnifiedMap.newMap(target.states().size());
        statePartition.forEach(part -> part.forEach(state -> stateMapping.put(state, partitionMapping.get(part))));

        statePartition.forEach(part -> {
            final S newState = partitionMapping.get(part);
            if (part.contains(startState())) {
                final S dummyStart = result.startState();
                result.setAsStart(newState);
                result.removeState(dummyStart);
            }
            if (part.anySatisfy(this::isAcceptState)) {
                result.setAsAccept(newState);
            }
            symbols.forEach(symbol -> {
                final S newSuccessor = stateMapping.get(part.getFirst().successor(symbol));
                result.addTransition(newState, newSuccessor, symbol);
            });
        });

        return result; // one-off
    }

    @Override
    default FSA<? extends State<T>, T> complement()
    {
        @SuppressWarnings("unchecked")
        final MutableFSA<MutableState<T>, T> result = (MutableFSA<MutableState<T>, T>) determinize().complete();
        final SetIterable<MutableState<T>> originAccepts = result.acceptStates();

        result.resetAcceptStates().setAllAsAccept(result.states().difference(originAccepts));

        return FSA.upcast(result); // shallow reference
    }

    @Override
    default FSA<? extends State<T>, T> intersect(FSA<? extends State<T>, T> target)
    {
        return FSA.upcast(this).product(FSA.upcast(target), alphabet(), Labels.matched(), (stateMapping, builder) -> {
            final BiMap<MutableState<T>, Pair<State<T>, State<T>>> mapping = stateMapping.inverse();
            builder.states().forEach(state -> {
                final State<T> state1 = mapping.get(state).getOne();
                final State<T> state2 = mapping.get(state).getTwo();
                if (this.isAcceptState(state1) && target.isAcceptState(state2)) {
                    builder.setAsAccept(state);
                }
            });
        }); // one-off
    }

    @Override
    default FSA<? extends State<T>, T> union(FSA<? extends State<T>, T> target)
    {
        if (!alphabet().equals(target.alphabet())) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }
        if (!states().containsAllIterable(target.states())) {
            throw new IllegalArgumentException(NONEXISTING_STATE);
        }

        final MutableFSA<S, T> result = FSAs.shallowCopy(this);
        @SuppressWarnings("unchecked")
        final MutableFSA<S, T> targetCasted = (MutableFSA<S, T>) target;
        final S newStart = result.newState();
        result.addEpsilonTransition(newStart, startState()).setAsStart(newStart) //
              .addStates(targetCasted.states()).addEpsilonTransition(newStart, targetCasted.startState());

        return FSA.upcast(result); // shallow reference
    }
}
