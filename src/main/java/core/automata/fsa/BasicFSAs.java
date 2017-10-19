package core.automata.fsa;

import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;

public class BasicFSAs implements FSA.Provider
{
    @Override
    public <S> FSA.Builder<S> builder(int stateCapacity, int symbolCapacity, S epsilonSymbol)
    {
        return new MapMapSetFSABuilder<>(stateCapacity, symbolCapacity, epsilonSymbol);
    }

    @Override
    public <S> FSA.Builder<S> builderOn(FSA<S> fsa)
    {
        return new MapMapSetFSABuilder<>((MapMapSetFSA<S>) fsa);
    }

    @Override
    public FSAManipulator manipulator()
    {
        return Manipulator.INSTANCE;
    }

    private static final class Manipulator
    {
        private static final FSAManipulator INSTANCE;

        static {
            INSTANCE = new MapMapSetFSAManipulator(new BaseFSAManipulator());
        }
    }
}
