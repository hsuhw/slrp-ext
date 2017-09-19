package api.automata;

import core.automata.MapMapDelta;
import core.automata.MapMapLikeDeltaBuilder;
import core.automata.MapMapSetDelta;

public final class DeltaFunctions
{
    static <S> DeltaFunction.Builder<S> builder(int stateNumberEstimate, S epsilonSymbol)
    {
        return new MapMapLikeDeltaBuilder<>(stateNumberEstimate, epsilonSymbol);
    }

    static <S> DeltaFunction.Builder<S> builderWith(DeltaFunction<S> other)
    {
        if (other instanceof MapMapDelta<?>) {
            return new MapMapLikeDeltaBuilder<>((MapMapDelta<S>) other);
        } else {
            return new MapMapLikeDeltaBuilder<>((MapMapSetDelta<S>) other);
        }
    }
}
