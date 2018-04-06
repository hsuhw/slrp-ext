package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

import java.util.function.Function;

import static common.util.Constants.DISPLAY_NEWLINE;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface State<S>
{
    String name();

    RichIterable<Pair<S, State<S>>> transitions();

    SetIterable<S> enabledSymbols();

    SetIterable<S> enabledSymbolsTo(State<S> state);

    boolean transitionExists(S transLabel);

    boolean transitionExists(State<S> state);

    SetIterable<State<S>> successors();

    SetIterable<State<S>> successors(S transLabel);

    default State<S> successor(S transLabel)
    {
        return successors(transLabel).getOnly();
    }

    MutableState<S> toMutable();

    default ImmutableState<S> toImmutable()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    String toString();

    default String toString(String indent, MapIterable<State<S>, String> nameMask)
    {
        final var result = new StringBuilder();
        final Function<State<S>, String> getName = state -> nameMask != null
                                                            ? nameMask.get(state)
                                                            : (state.name() != null ? state.name() : this.toString());

        transitions().forEach(labelAndState -> {
            result.append(indent);
            result.append(getName.apply(this)).append(" -> ").append(getName.apply(labelAndState.getTwo()));
            result.append(" [").append(labelAndState.getOne()).append("];");
            result.append(DISPLAY_NEWLINE);
        });

        return result.toString();
    }

    default String toString(String indent)
    {
        return toString(indent, null);
    }
}
