package api.automata.fsa;

import api.automata.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

import static api.util.Connectives.*;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface MutableFSA<S> extends MutableAutomaton<S>, FSA<S>
{
    @Override
    default FSA<S> trimUnreachableStates()
    {
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> unreachableStates = (SetIterable) unreachableStates();
        return FSAs.deepCopy(this).removeStates(unreachableStates);
    }

    @Override
    default <R> FSA<R> project(Alphabet<R> alphabet, Function<S, R> projector)
    {
        final MutableFSA<R> result = FSAs.create(alphabet, states().size()); // upper bound
        final MutableMap<State<S>, MutableState<R>> stateMapping = UnifiedMap.newMap(states().size());
        stateMapping.put(startState(), (MutableState<R>) result.startState());

        R newSymbol;
        for (State<S> dept : states()) {
            final MutableState<R> newDept = stateMapping.computeIfAbsent(dept, __ -> result.newState());
            for (S symbol : dept.enabledSymbols()) {
                for (State<S> dest : dept.successors(symbol)) {
                    if ((newSymbol = projector.apply(symbol)) != null) {
                        final MutableState<R> newDest = stateMapping.computeIfAbsent(dest, __ -> result.newState());
                        result.addTransition(newDept, newDest, newSymbol);
                    }
                }
            }
        }
        acceptStates().forEach(originAccept -> result.setAsAccept(stateMapping.get(originAccept)));

        return result.trimUnreachableStates(); // one-off
    }

    @Override
    default MutableFSA<S> toMutable()
    {
        return this;
    }

    @Override
    default MutableFSA<S> addSymbol(S symbol)
    {
        return (MutableFSA<S>) MutableAutomaton.super.addSymbol(symbol);
    }

    @Override
    MutableFSA<S> setAlphabet(Alphabet<S> alphabet);

    @Override
    MutableFSA<S> addState(MutableState<S> state);

    @Override
    default MutableFSA<S> addStates(RichIterable<MutableState<S>> states)
    {
        return (MutableFSA<S>) MutableAutomaton.super.addStates(states);
    }

    @Override
    MutableFSA<S> removeState(MutableState<S> state);

    @Override
    default MutableFSA<S> removeStates(RichIterable<MutableState<S>> states)
    {
        return (MutableFSA<S>) MutableAutomaton.super.removeStates(states);
    }

    @Override
    MutableFSA<S> setAsStart(MutableState<S> state);

    @Override
    MutableFSA<S> setAsAccept(MutableState<S> state);

    @Override
    MutableFSA<S> unsetAccept(MutableState<S> state);

    @Override
    default MutableFSA<S> setAllAsAccept(RichIterable<MutableState<S>> states)
    {
        return (MutableFSA<S>) MutableAutomaton.super.setAllAsAccept(states);
    }

    @Override
    MutableFSA<S> resetAcceptStates();

    @Override
    MutableFSA<S> addTransition(MutableState<S> dept, MutableState<S> dest, S symbol);

    @Override
    default MutableFSA<S> addEpsilonTransition(MutableState<S> dept, MutableState<S> dest)
    {
        return (MutableFSA<S>) MutableAutomaton.super.addEpsilonTransition(dept, dest);
    }

    @Override
    default FSA<S> determinize()
    {
        if (isDeterministic()) {
            return this;
        }

        final Automaton.TransitionGraph<S> delta = transitionGraph();
        final int capacity = states().size() * states().size(); // heuristic
        final MutableFSA<S> result = FSAs.create(alphabet(), capacity);
        final MutableMap<SetIterable<State<S>>, MutableState<S>> stateMapping = UnifiedMap.newMap(capacity);
        final Queue<SetIterable<State<S>>> pendingChecks = new LinkedList<>();

        final SetIterable<State<S>> startStates = delta.epsilonClosureOf(startState());
        stateMapping.put(startStates, (MutableState<S>) result.startState());
        pendingChecks.add(startStates);
        final SetIterable<S> symbols = alphabet().noEpsilonSet();
        SetIterable<State<S>> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            final MutableState<S> newDept = stateMapping.get(currStates);
            if (currStates.anySatisfy(this::isAcceptState)) {
                result.setAsAccept(newDept);
            }
            for (S symbol : symbols) {
                final SetIterable<State<S>> destStates = delta.epsilonClosureOf(currStates, symbol);
                final MutableState<S> newDest = stateMapping.computeIfAbsent(destStates, __ -> {
                    pendingChecks.add(destStates);
                    return result.newState();
                });
                result.addTransition(newDept, newDest, symbol);
            }
        }

        return result; // in-place or one-off
    }

    @Override
    default FSA<S> complete()
    {
        if (!this.isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }

        final SetIterable<State<S>> incomplete = incompleteStates();
        final SetIterable<S> completeAlphabet = alphabet().noEpsilonSet();
        if (incomplete.isEmpty()) {
            return this;
        }

        final MutableState<S> sink = newState();
        completeAlphabet.forEach(symbol -> addTransition(sink, sink, symbol));
        incomplete.forEach(state -> completeAlphabet.reject(state.enabledSymbols()::contains).forEach(
            nonTrans -> addTransition((MutableState<S>) state, sink, nonTrans)));

        return this; // in-place reference
    }

    @Override
    default FSA<S> minimize()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }
        if (acceptsNone()) {
            return FSAs.acceptingNone(alphabet());
        }
        if (complement().acceptsNone()) {
            return FSAs.acceptingAll(alphabet());
        }

        final FSA<S> target = trimUnreachableStates().complete();
        final ListIterable<State<S>> accepts = target.acceptStates().toList();
        final ListIterable<State<S>> nonAccepts = target.nonAcceptStates().toList();
        final RichIterable<ListIterable<State<S>>> initialPart = Lists.immutable.of(accepts, nonAccepts);
        final ListIterable<State<S>> initialCheck = accepts.size() < nonAccepts.size() ? accepts : nonAccepts;
        final RichIterable<ListIterable<State<S>>> statePartition = target.refinePartition(initialPart, initialCheck);

        final SetIterable<S> symbols = alphabet().noEpsilonSet();
        final MutableFSA<S> result = FSAs.create(alphabet(), statePartition.size());
        final MutableMap<ListIterable<State<S>>, MutableState<S>> partitionMapping = UnifiedMap
            .newMap(statePartition.size());
        statePartition.forEach(part -> partitionMapping.put(part, result.newState()));
        final MutableMap<State<S>, MutableState<S>> stateMapping = UnifiedMap.newMap(target.states().size());
        statePartition.forEach(part -> part.forEach(state -> stateMapping.put(state, partitionMapping.get(part))));

        statePartition.forEach(part -> {
            final MutableState<S> newState = partitionMapping.get(part);
            if (part.contains(startState())) {
                final State<S> dummyStart = result.startState();
                result.setAsStart(newState);
                result.removeState((MutableState<S>) dummyStart);
            }
            if (part.anySatisfy(this::isAcceptState)) {
                result.setAsAccept(newState);
            }
            symbols.forEach(symbol -> {
                final MutableState<S> newSuccessor = stateMapping.get(part.getFirst().successor(symbol));
                result.addTransition(newState, newSuccessor, symbol);
            });
        });

        return result; // one-off
    }

    @Override
    default FSA<S> complement()
    {
        final MutableFSA<S> result = FSAs.shallowCopy((MutableFSA<S>) determinize().complete());
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> nonAcceptStates = (SetIterable) nonAcceptStates();
        result.resetAcceptStates().setAllAsAccept(nonAcceptStates);

        return result; // shallow reference
    }

    @Override
    default FSA<S> intersect(FSA<S> target)
    {
        return (FSA<S>) product(target, alphabet(), Labels.matched(),
                                AcceptStates.select(this, target, AND)); // one-off
    }

    @Override
    default FSA<S> union(FSA<S> target)
    {
        if (!(target instanceof MutableFSA<?>) || !alphabet().equals(target.alphabet())) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        final MutableFSA<S> result = FSAs.shallowCopy(this);
        final MutableState<S> newStart = result.newState();
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> targetStates = (SetIterable) target.states();
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> targetAccepts = (SetIterable) target.acceptStates();
        result.addEpsilonTransition(newStart, (MutableState<S>) startState()).setAsStart(newStart) //
              .addStates(targetStates).addEpsilonTransition(newStart, (MutableState<S>) target.startState()) //
              .setAllAsAccept(targetAccepts);

        return result; // shallow reference
    }
}
