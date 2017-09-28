package api.automata;

import core.automata.MapMapDelta;
import core.automata.MapMapLikeDeltaBuilder;
import core.automata.MapMapSetDelta;

import static api.automata.DeltaFunction.Builder;

public final class DeltaFunctions
{
    public static <S> Builder<S> builder(int stateNumberEstimate, int symbolNumberEstimate, S epsilonSymbol)
    {
        return new MapMapLikeDeltaBuilder<>(stateNumberEstimate, symbolNumberEstimate, epsilonSymbol);
    }

    public static <S> Builder<S> builderBasedOn(DeltaFunction<S> delta)
    {
        return delta instanceof MapMapDelta<?>
               ? new MapMapLikeDeltaBuilder<>((MapMapDelta<S>) delta)
               : new MapMapLikeDeltaBuilder<>((MapMapSetDelta<S>) delta);
    }
}
