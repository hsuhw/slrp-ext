package core.automata;

import api.automata.StringSymbol;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import util.Misc;

public final class StringSymbols
{
    public static final String EPSILON_DISPLAY_VALUE = Misc.EPSILON_SYMBOL_DISPLAY_VALUE;
    public static final StringSymbol EPSILON = new StringSymbolImpl(Misc.EPSILON_SYMBOL_DISPLAY_VALUE);

    private StringSymbols()
    {
    }

    public static StringSymbol createOne(String displayValue)
    {
        return new StringSymbolImpl(displayValue);
    }

    public static ImmutableList<StringSymbol> of(String one, String two)
    {
        return Lists.immutable.of(new StringSymbolImpl(one), new StringSymbolImpl(two));
    }

    public static ImmutableList<StringSymbol> of(String one, String two, String three)
    {
        return Lists.immutable.of(new StringSymbolImpl(one), new StringSymbolImpl(two), new StringSymbolImpl(three));
    }

    public static ImmutableList<StringSymbol> of(String one, String two, String three, String four)
    {
        return Lists.immutable.of(new StringSymbolImpl(one), new StringSymbolImpl(two), new StringSymbolImpl(three),
                                  new StringSymbolImpl(four));
    }

    public static ImmutableList<StringSymbol> of(String one, String two, String three, String four, String five)
    {
        return Lists.immutable.of(new StringSymbolImpl(one), new StringSymbolImpl(two), new StringSymbolImpl(three),
                                  new StringSymbolImpl(four), new StringSymbolImpl(five));
    }

    public static ImmutableList<StringSymbol> of(String... symbols)
    {
        return Lists.immutable.of(symbols).collect(StringSymbolImpl::new);
    }
}
