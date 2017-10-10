package core.automata.fsa;

import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import api.automata.fsa.FSAs;
import api.synth.SatSolver;
import com.mscharhag.oleaster.runner.OleasterRunner;
import core.synth.ReferenceFSAEncoding;
import core.synth.Sat4jSolverAdapter;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

@RunWith(OleasterRunner.class)
public class BasicFSAManipulatorTest
{
    private final SatSolver solver = new Sat4jSolverAdapter();
    private final FSAManipulator manipulator = FSAs.manipulator();
    private AlphabetIntEncoder<String> alphabetEncoding;
    private ReferenceFSAEncoding<String> encoding;

    private void prepareAlphabet()
    {
        alphabetEncoding = AlphabetIntEncoders.create(Lists.mutable.of("e", "1", "2", "3"), "e");
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
                final ImmutableList<String> word1 = Lists.immutable.of("1", "3");
                final ImmutableList<String> word2 = Lists.immutable.of("2", "2");
                final ImmutableList<String> word3 = Lists.immutable.of("3", "1");
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<String> instance = manipulator.makeComplete(encoding.resolveToFSA());
                    expect(instance.isComplete()).toBeTrue();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeTrue();
                    expect(instance.accepts(word3)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("should handle complement-making correctly when delegated", () -> {
                final ImmutableList<String> word1 = Lists.immutable.of("1", "3");
                final ImmutableList<String> word2 = Lists.immutable.of("2", "2");
                final ImmutableList<String> word3 = Lists.immutable.of("3", "1");
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<String> instance = manipulator.makeComplement(encoding.resolveToFSA());
                    expect(instance.isComplete()).toBeTrue();
                    expect(instance.accepts(word1)).toBeFalse();
                    expect(instance.accepts(word2)).toBeFalse();
                    expect(instance.accepts(word3)).toBeTrue();
                    encoding.blockCurrentInstance();
                }
            });

            it("should handle intersection-making correctly when delegated", () -> {
                final ImmutableList<String> word1 = Lists.immutable.of("1", "3");
                final ImmutableList<String> word2 = Lists.immutable.of("2", "2");
                final ImmutableList<String> word3 = Lists.immutable.of("3", "1");
                encoding.ensureAcceptingWord(word1);
                encoding.ensureNotAcceptingWord(word2);
                final FSA<String> one = manipulator.makeComplement(encoding.resolveToFSA());
                encoding.ensureAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<String> two = encoding.resolveToFSA();
                    final FSA<String> instance = manipulator.makeIntersection(one, two);
                    expect(instance.accepts(word1)).toBeFalse();
                    expect(instance.accepts(word2)).toBeFalse();
                    expect(instance.accepts(word3)).toBeTrue();
                    encoding.blockCurrentInstance();
                }
            });

            it("should handle union-making correctly when delegated", () -> {
                final ImmutableList<String> word1 = Lists.immutable.of("1", "3");
                final ImmutableList<String> word2 = Lists.immutable.of("2", "2");
                final ImmutableList<String> word3 = Lists.immutable.of("3", "1");
                encoding.ensureAcceptingWord(word1);
                encoding.ensureNotAcceptingWord(word2);
                final FSA<String> one = manipulator.makeComplement(encoding.resolveToFSA());
                encoding.ensureAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<String> two = encoding.resolveToFSA();
                    final FSA<String> instance = manipulator.makeUnion(one, two);
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeTrue();
                    expect(instance.accepts(word3)).toBeTrue();
                    encoding.blockCurrentInstance();
                }
            });

        });
    }
}
