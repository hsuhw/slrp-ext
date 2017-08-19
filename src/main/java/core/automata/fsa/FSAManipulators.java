package core.automata.fsa;

import api.automata.fsa.FSAManipulator;

public final class FSAManipulators
{
    private FSAManipulators()
    {
    }

    public static FSAManipulator getDefault()
    {
        return DefaultManipulatorSingleton.INSTANCE;
    }

    private static final class DefaultManipulatorSingleton // Bill Pugh singleton pattern
    {
        private static final FSAManipulator INSTANCE;

        static {
            INSTANCE = new DoubleMapDFSAManipulator(new BaseFSAManipulator());
        }
    }
}
