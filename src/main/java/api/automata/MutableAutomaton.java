package api.automata;

import org.eclipse.collections.api.RichIterable;

import java.util.function.Function;

import static api.util.Constants.DISPLAY_STATE_NAME_PREFIX;
import static common.util.Constants.*;

public interface MutableAutomaton<S> extends Automaton<S>
{
    @Override
    TransitionGraph<S> transitionGraph();

    @Override
    default MutableAutomaton<S> toMutable()
    {
        return this;
    }

    default MutableAutomaton<S> addSymbol(S symbol)
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    MutableState<S> newState();

    MutableAutomaton<S> addState(MutableState<S> state);

    default MutableAutomaton<S> addStates(RichIterable<MutableState<S>> states)
    {
        states.forEach(this::addState);

        return this;
    }

    MutableAutomaton<S> removeState(MutableState<S> state);

    default MutableAutomaton<S> removeStates(RichIterable<MutableState<S>> states)
    {
        states.forEach(this::removeState);

        return this;
    }

    MutableAutomaton<S> setAsStart(MutableState<S> state);

    MutableAutomaton<S> setAsAccept(MutableState<S> state);

    MutableAutomaton<S> unsetAccept(MutableState<S> state);

    default MutableAutomaton<S> setAllAsAccept(RichIterable<MutableState<S>> states)
    {
        states.forEach(this::setAsAccept);

        return this;
    }

    MutableAutomaton<S> resetAcceptStates();

    MutableAutomaton<S> addTransition(MutableState<S> dept, MutableState<S> dest, S symbol);

    default MutableAutomaton<S> addEpsilonTransition(MutableState<S> dept, MutableState<S> dest)
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
            for (State<S> state : states()) {
                ((MutableState<S>) state).setName(DISPLAY_STATE_NAME_PREFIX + i);
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

    interface TransitionGraph<S> extends Automaton.TransitionGraph<S>
    {
        @Override
        String toString();
    }
}
