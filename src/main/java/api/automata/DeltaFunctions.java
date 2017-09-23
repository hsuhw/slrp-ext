package api.automata;

import core.automata.MapMapDelta;
import core.automata.MapMapLikeDeltaBuilder;
import core.automata.MapMapSetDelta;

import static api.automata.DeltaFunction.Builder;

public final class DeltaFunctions
{
    public static <S> Builder<S> builder(int stateNumberEstimate, S epsilonSymbol)
    {
        return new MapMapLikeDeltaBuilder<>(stateNumberEstimate, epsilonSymbol);
    }

    public static <S> Builder<S> builderOn(DeltaFunction<S> other)
    {
        if (other instanceof MapMapDelta<?>) {
            return new MapMapLikeDeltaBuilder<>((MapMapDelta<S>) other);
        } else {
            return new MapMapLikeDeltaBuilder<>((MapMapSetDelta<S>) other);
        }
    }
}
