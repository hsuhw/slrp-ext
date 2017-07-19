package synth;

import automata.fsa.FSA;
import automata.fsa.IntLabelFSA;
import automata.fsa.IntLabelRelationDefiner;

public class Synthesizer
{
    private SatSolver satSolver;
    private IntLabelFSA configSpaceAutomaton;
    private IntLabelRelationDefiner relationAutomaton;

    public Synthesizer(SatSolver satSolver)
    {
        this.satSolver = satSolver;
    }

    private void assertTargetExist(FSA tgt) throws UnsupportedOperationException
    {
        if (tgt == null) {
            throw new UnsupportedOperationException("try to get the FSA before it is synthesized");
        }
    }

    public FSA getConfigSpaceAutomata()
    {
        assertTargetExist(configSpaceAutomaton);
        return configSpaceAutomaton;
    }

    public FSA getRelationAutomaton()
    {
        assertTargetExist(relationAutomaton);
        return relationAutomaton;
    }

    public boolean synthesizeSuccessfully(){
        return false;
    }
}
