package api.automata;

import core.automata.NamedState;
import core.automata.NamelessState;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.primitive.IntInterval;

public final class States
{
    private States()
    {
    }

    public static State generate()
    {
        return new NamelessState();
    }

    public static ImmutableList<State> generate(int howMany)
    {
        if (howMany < 1) {
            throw new IllegalArgumentException("given number smaller than one");
        }
        return IntInterval.oneTo(howMany).collect(i -> new NamelessState());
    }

    public static State create(String name)
    {
        return new NamedState(name);
    }

    public static ImmutableList<State> of(String one, String two)
    {
        return Lists.immutable.of(new NamedState(one), new NamedState(two));
    }

    public static ImmutableList<State> of(String one, String two, String three)
    {
        return Lists.immutable.of(new NamedState(one), new NamedState(two), new NamedState(three));
    }

    public static ImmutableList<State> of(String one, String two, String three, String four)
    {
        return Lists.immutable
            .of(new NamedState(one), new NamedState(two), new NamedState(three), new NamedState(four));
    }

    public static ImmutableList<State> of(String one, String two, String three, String four, String five)
    {
        return Lists.immutable.of(new NamedState(one), new NamedState(two), new NamedState(three), new NamedState(four),
                                  new NamedState(five));
    }

    public static ImmutableList<State> of(String... states)
    {
        return Lists.immutable.of(states).collect(NamedState::new);
    }
}
