package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;

import java.util.function.Function;

import static api.util.Constants.DISPLAY_STATE_NAME_PREFIX;
import static common.util.Constants.*;

public interface MutableAutomaton<S extends MutableState<T>, T> extends Automaton<S, T>
{
    @Override
    Automaton<? extends State<T>, T> trimUnreachableStates();

    @Override
    <R> Automaton<? extends State<R>, R> project(Alphabet<R> alphabet, Function<T, R> projector);

    @Override
    <U extends State<V>, V, R> MutableAutomaton<? extends MutableState<R>, R> product(Automaton<U, V> target,
        Alphabet<R> alphabet, StepMaker<S, T, U, V, R> stepMaker, Finalizer<S, U, MutableState<R>, R> finalizer);

    @Override
    TransitionGraph<S, T> transitionGraph();

    @Override
    default MutableAutomaton<S, T> toMutable()
    {
        return this;
    }

    default MutableAutomaton<S, T> addSymbol(T symbol)
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    S newState();

    MutableAutomaton<S, T> addState(S state);

    default MutableAutomaton<S, T> addStates(RichIterable<S> states)
    {
        states.forEach(this::addState);

        return this;
    }

    MutableAutomaton<S, T> removeState(S state);

    default MutableAutomaton<S, T> removeStates(RichIterable<S> states)
    {
        states.forEach(this::removeState);

        return this;
    }

    MutableAutomaton<S, T> setAsStart(S state);

    MutableAutomaton<S, T> setAsAccept(S state);

    MutableAutomaton<S, T> unsetAccept(S state);

    default MutableAutomaton<S, T> setAllAsAccept(SetIterable<S> states)
    {
        states.forEach(this::setAsAccept);

        return this;
    }

    MutableAutomaton<S, T> resetAcceptStates();

    MutableAutomaton<S, T> addTransition(S dept, S dest, T symbol);

    default MutableAutomaton<S, T> addEpsilonTransition(S dept, S dest)
    {
        return addTransition(dept, dest, alphabet().epsilon());
    }

    @Override
    String toString();

    @Override
    default String toString(String indent, String nameTag)
    {
        if (states().anySatisfy(that -> that.name() == null)) {
            int i = 0;
            for (S state : states()) {
                state.setName(DISPLAY_STATE_NAME_PREFIX + i);
            }
        }

        final String innerIndent = indent + DISPLAY_INDENT;
        final String startState = startState().name();
        final String acceptStates = acceptStates().collect(State::name).makeString();
        final StringBuilder result = new StringBuilder();
        result.append(indent).append(nameTag).append(" {").append(DISPLAY_NEWLINE);
        result.append(innerIndent).append("start: ").append(startState).append(";").append(DISPLAY_NEWLINE);
        states().forEach(state -> result.append(state.toString(innerIndent)));
        result.append(innerIndent).append("accept: ").append(acceptStates).append(";").append(DISPLAY_NEWLINE);
        result.append(indent).append("}").append(DISPLAY_NEWLINE);

        return result.toString();
    }

    interface TransitionGraph<N extends MutableState<A>, A> extends Automaton.TransitionGraph<N, A>
    {
        @Override
        String toString();
    }
}
