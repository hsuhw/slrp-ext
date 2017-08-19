package core.automata.fsa;

import api.automata.Symbol;

public final class FSABuilders
{
    private FSABuilders()
    {
    }

    public static <S extends Symbol> BasicFSABuilder<S> createBasic(int symbolNumberEstimate, S epsilonSymbol,
                                                                    int stateNumberEstimate)
    {
        return new BasicFSABuilder<>(symbolNumberEstimate, epsilonSymbol, stateNumberEstimate);
    }
}
