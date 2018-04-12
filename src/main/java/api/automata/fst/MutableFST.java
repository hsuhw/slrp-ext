package api.automata.fst;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

import java.util.function.Function;

import static api.util.Connectives.*;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface MutableFST<S, T> extends MutableAutomaton<Pair<S, T>>, FST<S, T>
{
    @Override
    default FST<S, T> trimUnreachableStates()
    {
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<Pair<S, T>>> unreachableStates = (SetIterable) unreachableStates();
        return FSTs.deepCopy(this).removeStates(unreachableStates);
    }

    @Override
    default <R> Automaton<R> project(Alphabet<R> alphabet, Function<Pair<S, T>, R> projector)
    {
        final var capacity = states().size(); // upper bound
        if (alphabet.epsilon() instanceof Pair<?, ?>) {
            @SuppressWarnings("unchecked")
            final MutableAutomaton<R> result = FSTs.create((Alphabet) alphabet, capacity);
            return projectInto(result, projector);
        } else {
            final var result = FSAs.create(alphabet, capacity);
            return projectInto(result, projector);
        }
    }

    @Override
    default MutableFST<S, T> toMutable()
    {
        return this;
    }

    @Override
    default MutableFST<S, T> addSymbol(Pair<S, T> symbol)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.addSymbol(symbol);
    }

    @Override
    MutableFST<S, T> setAlphabet(Alphabet<Pair<S, T>> alphabet);

    @Override
    MutableFST<S, T> addState(MutableState<Pair<S, T>> state);

    @Override
    default MutableFST<S, T> addStates(RichIterable<MutableState<Pair<S, T>>> states)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.addStates(states);
    }

    @Override
    MutableFST<S, T> removeState(MutableState<Pair<S, T>> state);

    @Override
    default MutableFST<S, T> removeStates(RichIterable<MutableState<Pair<S, T>>> states)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.removeStates(states);
    }

    @Override
    MutableFST<S, T> setAsStart(MutableState<Pair<S, T>> state);

    @Override
    MutableFST<S, T> setAsAccept(MutableState<Pair<S, T>> state);

    @Override
    MutableFST<S, T> unsetAccept(MutableState<Pair<S, T>> state);

    @Override
    default MutableFST<S, T> setAllAsAccept(RichIterable<MutableState<Pair<S, T>>> states)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.setAllAsAccept(states);
    }

    @Override
    MutableFST<S, T> resetAcceptStates();

    @Override
    MutableFST<S, T> addTransition(MutableState<Pair<S, T>> dept, MutableState<Pair<S, T>> dest, Pair<S, T> symbol);

    @Override
    default MutableFST<S, T> addEpsilonTransition(MutableState<Pair<S, T>> dept, MutableState<Pair<S, T>> dest)
    {
        return (MutableFST<S, T>) MutableAutomaton.super.addEpsilonTransition(dept, dest);
    }

    @Override
    default FST<S, T> intersect(FST<S, T> target)
    {
        return (FST<S, T>) product(target, alphabet(), Labels.matched(), AcceptStates.select(this, target, AND));
    }

    @Override
    default FST<S, T> union(FST<S, T> target)
    {
        if (!(target instanceof MutableFST<?, ?>) || !alphabet().equals(target.alphabet())) {
            throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
        }

        final var result = FSTs.shallowCopy(this);
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<Pair<S, T>>> targetStates = (SetIterable) target.states();
        @SuppressWarnings("unchecked")
        final SetIterable<MutableState<Pair<S, T>>> targetAccepts = (SetIterable) target.acceptStates();
        result.addStates(targetStates).setAllAsAccept(targetAccepts);

        final var newStart = result.newState();
        final Procedure<Pair<Pair<S, T>, State<Pair<S, T>>>> addToNewStart = symbolAndDest -> //
            result.addTransition(newStart, (MutableState<Pair<S, T>>) symbolAndDest.getTwo(), symbolAndDest.getOne());
        startState().transitions().forEach(addToNewStart);
        target.startState().transitions().forEach(addToNewStart);
        result.setAsStart(newStart);

        return result; // shallow reference
    }

    @Override
    default FSA<Pair<S, T>> asFSA()
    {
        return FSAs.castFrom(this);
    }
}
