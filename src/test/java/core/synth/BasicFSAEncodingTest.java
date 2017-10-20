package core.synth;

import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.fsa.FSA;
import api.synth.SatSolver;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

@RunWith(OleasterRunner.class)
public class BasicFSAEncodingTest
{
    private final SatSolver solver = new Sat4jSolverAdapter();
    private AlphabetIntEncoder<String> alphabetEncoding;
    private BasicFSAEncoding<String> encoding;

    private void prepareAlphabet()
    {
        alphabetEncoding = AlphabetIntEncoders.create(Lists.mutable.of("e", "1", "2"), "e");
    }

    private void prepareFSAEncoding()
    {
        encoding = new BasicFSAEncoding<>(solver, 2, alphabetEncoding);
        encoding.ensureNoUnreachableState();
        encoding.ensureNoUnreachableState(); // should be cached, hard to tell though
        encoding.ensureNoDeadEndState();
        encoding.ensureNoDeadEndState(); // should be cached, hard to tell though
    }

    {
        prepareAlphabet();

        describe("No-dangling ensured", () -> {

            beforeEach(() -> {
                solver.reset();
                prepareFSAEncoding();
            });

            it("finds correct FSAs with accepting or not constraints", () -> {
                final ImmutableList<String> word1 = Lists.immutable.of("1", "2");
                final ImmutableList<String> word2 = Lists.immutable.of("2", "2");
                final ImmutableList<String> word3 = Lists.immutable.of("2", "1");
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<String> instance = encoding.resolve();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeTrue();
                    expect(instance.accepts(word3)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("finds correct FSAs with whether-accept constraints", () -> {
                final ImmutableList<String> word1 = Lists.immutable.of("1", "2");
                final ImmutableList<String> word2 = Lists.immutable.of("2", "2");
                final int yes = solver.newFreeVariables(1).getFirst();
                solver.setLiteralTruthy(yes);
                final int no = solver.newFreeVariables(1).getFirst();
                solver.setLiteralFalsy(no);
                encoding.whetherAcceptWord(yes, word1);
                encoding.whetherAcceptWord(no, word2);

                while (solver.findItSatisfiable()) {
                    final FSA<String> instance = encoding.resolve();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("finds correct FSAs with no-words-purely-made-of constraints", () -> {
                encoding.ensureNoWordPurelyMadeOf(alphabetEncoding.originAlphabet().set());
                expect(solver.findItSatisfiable()).toBeFalse();
            });

            it("finds no FSAs when constraints UNSAT", () -> {
                final ImmutableList<String> word1 = Lists.immutable.of("1", "2");
                encoding.ensureAcceptingWord(word1);
                encoding.ensureNotAcceptingWord(word1);
                expect(solver.findItSatisfiable()).toBeFalse();
                expect(encoding.resolve()).toBeNull();
            });

        });
    }
}
