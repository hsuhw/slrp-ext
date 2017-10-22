package core.synth;

import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.fsa.FSA;
import api.synth.FSAEncoding;
import api.synth.SatSolver;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractFSAEncodingTest
{
    protected final SatSolver solver = new Sat4jSolverAdapter();
    protected final AlphabetIntEncoder<Object> alphabetEncoding;
    protected FSAEncoding<Object> encoding;

    protected abstract FSAEncoding<Object> encodingForCommonTest();

    {
        final Object e = new Object();
        final Object a1 = new Object();
        final Object a2 = new Object();
        alphabetEncoding = AlphabetIntEncoders.create(Lists.mutable.of(e, a1, a2), e);

        describe("No-dangling ensured", () -> {

            beforeEach(() -> {
                solver.reset();
                encoding = encodingForCommonTest();
                encoding.ensureNoUnreachableState();
                encoding.ensureNoUnreachableState(); // should be cached, hard to tell though
                encoding.ensureNoDeadEndState();
                encoding.ensureNoDeadEndState(); // should be cached, hard to tell though
            });

            it("finds correct FSAs with accepting or not constraints", () -> {
                final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
                final ImmutableList<Object> word2 = Lists.immutable.of(a2, a2);
                final ImmutableList<Object> word3 = Lists.immutable.of(a2, a1);
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findItSatisfiable()) {
                    final FSA<Object> instance = encoding.resolve();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeTrue();
                    expect(instance.accepts(word3)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("finds correct FSAs with whether-accept constraints", () -> {
                final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
                final ImmutableList<Object> word2 = Lists.immutable.of(a2, a2);
                final int yes = solver.newFreeVariables(1).getFirst();
                solver.setLiteralTruthy(yes);
                final int no = solver.newFreeVariables(1).getFirst();
                solver.setLiteralFalsy(no);
                encoding.whetherAcceptWord(yes, word1);
                encoding.whetherAcceptWord(no, word2);

                while (solver.findItSatisfiable()) {
                    final FSA<Object> instance = encoding.resolve();
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
                final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
                encoding.ensureAcceptingWord(word1);
                encoding.ensureNotAcceptingWord(word1);
                expect(solver.findItSatisfiable()).toBeFalse();
                expect(encoding.resolve()).toBeNull();
            });

        });
    }
}
