package api.automata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;

import static common.util.Constants.DISPLAY_NEWLINE;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface State<T>
{
    String name();

    RichIterable<? extends Pair<T, ? extends State<T>>> transitions();

    SetIterable<T> enabledSymbols();

    SetIterable<T> enabledSymbolsTo(State<T> state);

    boolean transitionExists(T transLabel);

    boolean transitionExists(State<T> state);

    SetIterable<? extends State<T>> successors();

    SetIterable<? extends State<T>> successors(T transLabel);

    default State<T> successor(T transLabel)
    {
        return successors().getOnly();
    }

    MutableState<T> toMutable();

    default ImmutableState<T> toImmutable()
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
