package core.automata;

import api.automata.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.primitive.IntInterval;

public final class States
{
    private States()
    {
    }

    public static State generateOne()
    {
        return new GeneratedState();
    }

    public static ImmutableList<State> generate(int howMany)
    {
        if (howMany < 1) {
            throw new IllegalArgumentException("given number smaller than one");
        }
        return IntInterval.oneTo(howMany).collect(i -> new GeneratedState());
    }

    public static State createOne(String name)
    {
        return new ParsedState(name);
    }

    public static ImmutableList<State> of(String one, String two)
    {
        return Lists.immutable.of(new ParsedState(one), new ParsedState(two));
    }

    public static ImmutableList<State> of(String one, String two, String three)
    {
        return Lists.immutable.of(new ParsedState(one), new ParsedState(two), new ParsedState(three));
    }

    public static ImmutableList<State> of(String one, String two, String three, String four)
    {
        return Lists.immutable
            .of(new ParsedState(one), new ParsedState(two), new ParsedState(three), new ParsedState(four));
    }

    public static ImmutableList<State> of(String one, String two, String three, String four, String five)
    {
        return Lists.immutable
            .of(new ParsedState(one), new ParsedState(two), new ParsedState(three), new ParsedState(four),
                new ParsedState(five));
    }

    public static ImmutableList<State> of(String... states)
    {
        return Lists.immutable.of(states).collect(ParsedState::new);
    }
}
