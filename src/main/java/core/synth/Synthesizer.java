//package synth;
//
//import automata.fsa.FSA;
//import automata.fsa.IntLabelFSA;
//import automata.fsa.IntLabelRelationDefiner;
//import org.eclipse.collections.api.list.primitive.ImmutableIntList;
//
//public class Synthesizer
//{
//    private SatSolver satSolver;
//    private IntLabelFSA configRangeAutomaton;
//    private IntLabelRelationDefiner relationAutomaton;
//
//    public Synthesizer(SatSolver satSolver)
//    {
//        this.satSolver = satSolver;
//    }
//
//    private void assertTargetExist(FSA tgt) throws IllegalArgumentException
//    {
//        if (tgt == null) {
//            throw new IllegalArgumentException("try to get the FSA before it is synthesized");
//        }
//    }
//
//    public IntLabelFSA getConfigRangeAutomaton()
//    {
//        assertTargetExist(configRangeAutomaton);
//        return configRangeAutomaton;
//    }
//
//    public IntLabelRelationDefiner getRelationAutomaton()
//    {
//        assertTargetExist(relationAutomaton);
//        return relationAutomaton;
//    }
//
//    public boolean synthesizeSuccessfully()
//    {
//        return satSolver.findItSatisfiable();
//    }
//
//    public void ensureConfigRangeHasMember(ImmutableIntList member)
//    {
//    }
//}
