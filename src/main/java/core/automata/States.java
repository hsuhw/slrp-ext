package core.automata;

import api.automata.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public final class States
{
    private States()
    {
    }

    public static State createOne(String name)
    {
        return new StateImpl(name);
    }

    public static ImmutableList<State> of(String one, String two)
    {
        return Lists.immutable.of(new StateImpl(one), new StateImpl(two));
    }

    public static ImmutableList<State> of(String one, String two, String three)
    {
        return Lists.immutable.of(new StateImpl(one), new StateImpl(two), new StateImpl(three));
    }

    public static ImmutableList<State> of(String one, String two, String three, String four)
    {
        return Lists.immutable.of(new StateImpl(one), new StateImpl(two), new StateImpl(three), new StateImpl(four));
    }

    public static ImmutableList<State> of(String one, String two, String three, String four, String five)
    {
        return Lists.immutable
            .of(new StateImpl(one), new StateImpl(two), new StateImpl(three), new StateImpl(four), new StateImpl(five));
    }

    public static ImmutableList<State> of(String... states)
    {
        return Lists.immutable.of(states).collect(StateImpl::new);
    }
}
