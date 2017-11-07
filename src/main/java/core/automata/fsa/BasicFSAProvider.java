package core.automata.fsa;

import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;

import java.util.LinkedList;
import java.util.List;

public class BasicFSAProvider implements FSA.Provider
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
            final List<Class<? extends FSAManipulator.Decorator>> decorators = new LinkedList<>();
            decorators.add(BasicFSAManipulator.class); // order matters

            FSAManipulator decoratee = new BaseFSAManipulator();
            try {
                for (Class<? extends FSAManipulator.Decorator> decorator : decorators) {
                    decoratee = decorator.getConstructor(FSAManipulator.class).newInstance(decoratee);
                }
            } catch (Exception e) {
                throw new IllegalStateException();
            }

            INSTANCE = decoratee;
        }
    }
}
