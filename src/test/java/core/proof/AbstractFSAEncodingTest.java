package core.proof;

import api.automata.AlphabetIntEncoder;
import api.automata.AlphabetIntEncoders;
import api.automata.fsa.FSA;
import api.proof.FSAEncoding;
import common.sat.Sat4jSolverAdapter;
import common.sat.SatSolver;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractFSAEncodingTest
{
    protected final SatSolver solver = new Sat4jSolverAdapter();
    protected FSAEncoding<Object> encoding;

    protected abstract FSAEncoding<Object> newEncoding(int size, AlphabetIntEncoder<Object> alphabetEncoding);

    {
        final var e = new Object();
        final var a1 = new Object();
        final var a2 = new Object();
        final var alphabet1 = AlphabetIntEncoders.create(Lists.mutable.of(e, a1), e);
        final var alphabet2 = AlphabetIntEncoders.create(Lists.mutable.of(e, a1, a2), e);

        describe("To ignore shapeless targets", () -> {

            beforeEach(() -> {
                encoding = newEncoding(2, alphabet1);
            });

            it("can ensure no unreachable state", () -> {
                FSA<Object> fsa;
                var count = 0;
                encoding.ensureNoUnreachableState();
                while (solver.findItSatisfiable()) {
                    fsa = encoding.resolve();
                    expect(fsa.unreachableStates().isEmpty()).toBeTrue();
                    count++;
                    encoding.blockCurrentInstance();
                }
                expect(count).toEqual(9);
            });

            it("can ensure no dead-end state", () -> {
                FSA<Object> fsa;
                var count = 0;
                encoding.ensureNoDeadEndState();
                while (solver.findItSatisfiable()) {
                    fsa = encoding.resolve();
                    expect(fsa.deadEndStates().isEmpty()).toBeTrue();
                    count++;
                    encoding.blockCurrentInstance();
                }
                expect(count).toEqual(14);
            });

            it("can ensure no dangling state", () -> {
                FSA<Object> fsa;
                var count = 0;
                encoding.ensureNoDanglingState();
                while (solver.findItSatisfiable()) {
                    fsa = encoding.resolve();
                    expect(fsa.danglingStates().isEmpty()).toBeTrue();
                    count++;
                    encoding.blockCurrentInstance();
                }
                expect(count).toEqual(7);
            });

        });

        describe("No-dangling", () -> {

            describe("set up ahead", () -> {

                final var word1 = Lists.immutable.of(a1, a1, a1, a1, a1);
                final var word2 = Lists.immutable.of(a1, a1);
                final var word3 = Lists.immutable.of(a1, a2, a1);
                final var word4 = Lists.immutable.of(a2, a1, a2);

                beforeEach(() -> {
                    encoding = newEncoding(3, alphabet1);
                    encoding.ensureNoDanglingState();
                });

                it("can show all on accepting a word", () -> {
                    encoding.ensureAccepting(word1);
                    FSA<Object> fsa;
                    var count = 0;
                    while (solver.findItSatisfiable()) {
                        fsa = encoding.resolve();
                        expect(fsa.accepts(word1)).toBeTrue();
                        count++;
                        encoding.blockCurrentInstance();
                    }
                    expect(count).toEqual(16);
                });

                it("can show all on not accepting a word", () -> {
                    encoding.ensureNoAccepting(word1);
                    FSA<Object> fsa;
                    var count = 0;
                    while (solver.findItSatisfiable()) {
                        fsa = encoding.resolve();
                        expect(fsa.accepts(word1)).toBeFalse();
                        count++;
                        encoding.blockCurrentInstance();
                    }
                    expect(count).toEqual(12);
                });

                it("can show all on iff-accepting a word", () -> {
                    final var yes = solver.newFreeVariable();
                    solver.setLiteralTruthy(yes);
                    encoding.ensureAcceptingIfOnlyIf(-yes, word1);
                    encoding.ensureAcceptingIfOnlyIf(yes, word2);
                    FSA<Object> fsa;
                    var count = 0;
                    while (solver.findItSatisfiable()) {
                        fsa = encoding.resolve();
                        expect(fsa.accepts(word1)).toBeFalse();
                        expect(fsa.accepts(word2)).toBeTrue();
                        count++;
                        encoding.blockCurrentInstance();
                    }
                    expect(count).toEqual(8);
                });

                it("can show all on no-purely-made-of", () -> {
                    encoding = newEncoding(2, alphabet2);
                    encoding.ensureNoDanglingState();
                    encoding.ensureAccepting(word3);
                    encoding.ensureAccepting(word4);
                    encoding.ensureNoWordPurelyMadeOf(Sets.immutable.of(a1));
                    FSA<Object> fsa;
                    var count = 0;
                    while (solver.findItSatisfiable()) {
                        fsa = encoding.resolve();
                        expect(fsa.accepts(word3)).toBeTrue();
                        expect(fsa.accepts(word4)).toBeTrue();
                        count++;
                        encoding.blockCurrentInstance();
                    }
                    expect(count).toEqual(1);
                });

            });

        });
    }
}
