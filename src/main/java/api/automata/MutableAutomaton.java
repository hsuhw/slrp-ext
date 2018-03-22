package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import static api.util.Constants.DISPLAY_DUMMY_STATE_NAME_PREFIX;
import static common.util.Constants.*;

public interface MutableAutomaton<S> extends Automaton<S>
{
    @Override
    MutableState<S> startState();

    @Override
    TransitionGraph<S> transitionGraph();

    @Override
    default MutableAutomaton<S> toMutable()
    {
        return this;
    }

    MutableAutomaton<S> setAlphabet(Alphabet<S> alphabet);

    default MutableAutomaton<S> addSymbol(S symbol)
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    MutableState<S> newState();

    default MutableState<S> newState(String name)
    {
        return newState().setName(name);
    }

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
        MutableMap<State<S>, String> nameMask = null;
        Function<State<S>, String> getName = state -> state.name() != null ? state.name() : this.toString();
        if (states().anySatisfy(that -> that.name() == null)) {
            nameMask = UnifiedMap.newMap(states().size()); // upper bound
            int i = 0;
            for (State<S> state : states()) {
                nameMask.put(state, state.name() == null ? DISPLAY_DUMMY_STATE_NAME_PREFIX + i++ : state.name());
            }
            getName = nameMask::get;
        }

        final String innerIndent = indent + DISPLAY_INDENT;
        final String startState = getName.apply(startState());
        final String acceptStates = acceptStates().collect(getName).makeString();
        final StringBuilder result = new StringBuilder();
        result.append(indent).append(nameTag).append(nameTag.equals("") ? "{" : " {").append(DISPLAY_NEWLINE);
        result.append(innerIndent).append("start: ").append(startState).append(";").append(DISPLAY_NEWLINE);
        for (State<S> state : states()) {
            result.append(state.toString(innerIndent, nameMask));
        }
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
