package core.automata.fsa;

import api.automata.IntAlphabetTranslator;
import api.automata.StringSymbol;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.synth.SatSolver;
import com.mscharhag.oleaster.runner.OleasterRunner;
import core.automata.AlphabetTranslators;
import core.automata.StringSymbols;
import core.synth.ReferenceFSAEncoding;
import core.synth.Sat4jSolverAdapter;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

@RunWith(OleasterRunner.class)
public class DoubleMapDFSAManipulatorTest
{
    private final SatSolver solver = new Sat4jSolverAdapter();
    private final FSAManipulator manipulator = FSAManipulators.getDefault();
    private IntAlphabetTranslator<StringSymbol> alphabetEncoding;
    private ReferenceFSAEncoding<StringSymbol> encoding;

    private void prepareAlphabet()
    {
        final ImmutableList<StringSymbol> definition = StringSymbols.of("e", "a1", "a2", "a3");
        alphabetEncoding = AlphabetTranslators.createIntOne(definition, definition.get(0));
    }

    private void prepareFSAEncoding()
    {
        encoding = new ReferenceFSAEncoding<>(solver, 2, alphabetEncoding);
        encoding.ensureNoUnreachableStates();
        encoding.ensureNoDeadEndStates();
    }

    {
        prepareAlphabet();

        describe("Assuming the basic encoding generate correct DFSAs", () -> {

            beforeEach(() -> {
                solver.reset();
                prepareFSAEncoding();
            });

            it("should handle complete-making correctly when delegated", () -> {
                final ImmutableList<StringSymbol> word1 = alphabetEncoding.translateBack(1, 3);
                final ImmutableList<StringSymbol> word2 = alphabetEncoding.translateBack(2, 2);
                final ImmutableList<StringSymbol> word3 = alphabetEncoding.translateBack(3, 1);
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<StringSymbol> instance = manipulator.makeComplete(encoding.resolveToFSA());
                    expect(instance.isComplete()).toBeTrue();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeTrue();
                    expect(instance.accepts(word3)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("should handle complement-making correctly when delegated", () -> {
                final ImmutableList<StringSymbol> word1 = alphabetEncoding.translateBack(1, 3);
                final ImmutableList<StringSymbol> word2 = alphabetEncoding.translateBack(2, 2);
                final ImmutableList<StringSymbol> word3 = alphabetEncoding.translateBack(3, 1);
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<StringSymbol> instance = manipulator.makeComplement(encoding.resolveToFSA());
                    expect(instance.isComplete()).toBeTrue();
                    expect(instance.accepts(word1)).toBeFalse();
                    expect(instance.accepts(word2)).toBeFalse();
                    expect(instance.accepts(word3)).toBeTrue();
                    encoding.blockCurrentInstance();
                }
            });

        });
    }
}
