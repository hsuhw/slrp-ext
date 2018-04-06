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
        if (unreachableStates().isEmpty()) {
            return this; // in-place reference
        }

        final var result = FSAs.deepCopy(this);
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> unreachableStates = (SetIterable) result.unreachableStates();

        return result.removeStates(unreachableStates); // one-off
    }

    @Override
    default <R> FSA<R> project(Alphabet<R> alphabet, Function<S, R> projector)
    {
        final var result = FSAs.create(alphabet, states().size()); // upper bound

        return (FSA<R>) projectInto(result, projector);
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
            return this; // in-place reference
        }

        final Automaton.TransitionGraph<S> delta = transitionGraph();
        final var capacityComputed = states().size() * states().size(); // heuristic
        final var capacity = capacityComputed < 0 ? Integer.MAX_VALUE : capacityComputed;
        final var result = FSAs.create(alphabet(), capacity);
        final MutableMap<SetIterable<State<S>>, MutableState<S>> stateMapping = UnifiedMap.newMap(capacity);
        final Queue<SetIterable<State<S>>> pendingChecks = new LinkedList<>();

        final var startStates = delta.epsilonClosureOf(startState());
        stateMapping.put(startStates, result.startState());
        pendingChecks.add(startStates);
        final var symbols = alphabet().noEpsilonSet();
        SetIterable<State<S>> currStates;
        while ((currStates = pendingChecks.poll()) != null) {
            final var newDept = stateMapping.get(currStates);
            if (currStates.anySatisfy(this::isAcceptState)) {
                result.setAsAccept(newDept);
            }
            for (var symbol : symbols) {
                final var destStates = delta.epsilonClosureOf(currStates, symbol);
                final var newDest = stateMapping.computeIfAbsent(destStates, __ -> {
                    pendingChecks.add(destStates);
                    return result.newState();
                });
                result.addTransition(newDept, newDest, symbol);
            }
        }

        return result; // one-off
    }

    @Override
    default FSA<S> complete()
    {
        if (!this.isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }
        if (incompleteStates().isEmpty()) {
            return this; // in-place reference
        }

        final var result = FSAs.deepCopy(this);
        final var completeAlphabet = alphabet().noEpsilonSet();
        final var incomplete = result.incompleteStates();

        final var sink = result.newState();
        completeAlphabet.forEach(symbol -> result.addTransition(sink, sink, symbol));
        incomplete.forEach(state -> completeAlphabet.reject(state.enabledSymbols()::contains).forEach(
            nonTrans -> result.addTransition((MutableState<S>) state, sink, nonTrans)));

        return result; // one-off
    }

    @Override
    default FSA<S> minimize()
    {
        if (!isDeterministic()) {
            throw new UnsupportedOperationException("only available on deterministic instances");
        }
        if (acceptsNone()) {
            return FSAs.acceptingNone(alphabet()); // shared reference
        }
        if (complement().acceptsNone()) {
            return FSAs.acceptingAll(alphabet()); // shared reference
        }

        final var target = trimUnreachableStates().complete();
        final ListIterable<State<S>> accepts = target.acceptStates().toList();
        final ListIterable<State<S>> nonAccepts = target.nonAcceptStates().toList();
        final RichIterable<ListIterable<State<S>>> initialPart = Lists.immutable.of(accepts, nonAccepts);
        final var initialCheck = accepts.size() < nonAccepts.size() ? accepts : nonAccepts;
        final var statePartition = target.refinePartition(initialPart, initialCheck);

        final var symbols = alphabet().noEpsilonSet();
        final var result = FSAs.create(alphabet(), statePartition.size());
        final MutableMap<ListIterable<State<S>>, MutableState<S>> partitionMapping = UnifiedMap
            .newMap(statePartition.size());
        statePartition.forEach(part -> partitionMapping.put(part, result.newState()));
        final MutableMap<State<S>, MutableState<S>> stateMapping = UnifiedMap.newMap(target.states().size());
        statePartition.forEach(part -> part.forEach(state -> stateMapping.put(state, partitionMapping.get(part))));

        final var originalStart = target.startState();
        statePartition.forEach(part -> {
            final var newState = partitionMapping.get(part);
            if (part.contains(originalStart)) {
                final var dummyStart = result.startState();
                result.setAsStart(newState);
                result.removeState(dummyStart);
            }
            if (part.anySatisfy(target::isAcceptState)) {
                result.setAsAccept(newState);
            }
            symbols.forEach(symbol -> {
                final var newSuccessor = stateMapping.get(part.getFirst().successor(symbol));
                result.addTransition(newState, newSuccessor, symbol);
            });
        });

        return result; // one-off
    }

    @Override
    default FSA<S> complement()
    {
        final var completed = (MutableFSA<S>) determinize().complete();
        final var result = FSAs.shallowCopy(completed);
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> nonAcceptStates = (SetIterable) result.nonAcceptStates();
        result.resetAcceptStates().setAllAsAccept(nonAcceptStates);

        return result; // shallow reference
    }

    @Override
    default FSA<S> intersect(FSA<S> target)
    {
        return (FSA<S>) product(target, alphabet(), Labels.matched(), AcceptStates.select(this, target, AND));
    }

    @Override
    default FSA<S> union(FSA<S> target)
    {
        if (!(target instanceof MutableFSA<?>) || !alphabet().equals(target.alphabet())) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        final var result = FSAs.shallowCopy(this);
        final var newStart = result.newState();
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> targetStates = (SetIterable) target.states();
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<S>> targetAccepts = (SetIterable) target.acceptStates();
        result.addEpsilonTransition(newStart, startState()).setAsStart(newStart) //
              .addStates(targetStates).addEpsilonTransition(newStart, (MutableState<S>) target.startState()) //
              .setAllAsAccept(targetAccepts);

        return result; // shallow reference
    }
}
