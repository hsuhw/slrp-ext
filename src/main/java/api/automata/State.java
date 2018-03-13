package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

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

    default String toString(String indent, String nameTag)
    {
        final StringBuilder result = new StringBuilder();

        transitions().forEach(labelAndState -> {
            result.append(indent);
            result.append(nameTag).append(" -> ").append(labelAndState.getTwo().name());
            result.append(" [").append(labelAndState.getOne()).append("];");
            result.append(DISPLAY_NEWLINE);
        });

        return result.toString();
    }

    default String toString(String indent)
    {
        if (name() == null) { // expected to be used as parts
            throw new IllegalStateException("no display name available");
        }

        return toString(indent, name());
    }
}
