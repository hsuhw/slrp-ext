package core.synth;

import api.automata.IntAlphabetTranslator;
import api.automata.fsa.FSA;
import api.synth.SatSolver;
import com.mscharhag.oleaster.runner.OleasterRunner;
import core.automata.StringSymbol;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

@RunWith(OleasterRunner.class)
public class BasicFSAEncodingTest
{
    private final SatSolver solver = new Sat4jSolverAdapter();
    private IntAlphabetTranslator<StringSymbol> alphabetEncoding;
    private BasicFSAEncoding<StringSymbol> encoding;

    private void prepareAlphabet()
    {
        final StringSymbol[] symbols = new StringSymbol[4];
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = new StringSymbol("a" + i);
        }
        final ImmutableList<StringSymbol> definition = Lists.immutable.of(symbols);
        alphabetEncoding = new core.automata.IntAlphabetTranslator<>(definition, symbols[0]);
    }

    {
        prepareAlphabet();

        describe("When NUS, NDES are ensured", () -> {

            beforeEach(() -> {
                solver.reset();
                encoding = new BasicFSAEncoding<>(solver, 2, alphabetEncoding);
                encoding.ensureNoUnreachableStates();
                encoding.ensureNoDeadEndStates();
            });

            it("should find correct FSAs with accepting (or not accepting) constraints", () -> {
                final ImmutableList<StringSymbol> word1 = alphabetEncoding.translateBack(1, 3);
                final ImmutableList<StringSymbol> word2 = alphabetEncoding.translateBack(2, 2);
                final ImmutableList<StringSymbol> word3 = alphabetEncoding.translateBack(3, 1);
                encoding.ensureAcceptingWord(word1);
                encoding.ensureAcceptingWord(word2);
                encoding.ensureNotAcceptingWord(word3);

                while (solver.findModel() != null) {
                    final FSA<StringSymbol> instance = encoding.toFSA();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeTrue();
                    expect(instance.accepts(word3)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("should find correct FSAs with whether-accept constraints", () -> {
                final ImmutableList<StringSymbol> word1 = alphabetEncoding.translateBack(1, 3);
                final ImmutableList<StringSymbol> word2 = alphabetEncoding.translateBack(2, 2);
                final int yes = solver.newFreeVariables(1).getFirst();
                solver.setLiteralTruthy(yes);
                final int no = solver.newFreeVariables(1).getFirst();
                solver.setLiteralFalsy(no);
                encoding.whetherAcceptWord(yes, word1);
                encoding.whetherAcceptWord(no, word2);

                while (solver.findModel() != null) {
                    final FSA<StringSymbol> instance = encoding.toFSA();
                    expect(instance.accepts(word1)).toBeTrue();
                    expect(instance.accepts(word2)).toBeFalse();
                    encoding.blockCurrentInstance();
                }
            });

            it("should find correct FSAs with no-words-purely-made-of constraints", () -> {
                encoding.ensureNoWordsPurelyMadeOf(alphabetEncoding.getOriginAlphabet());
                while (solver.findModel() != null) {
                    expect(encoding.toFSA().getTransitionFunction().size()).toEqual(0);
                    encoding.blockCurrentInstance();
                }
            });

            it("should find no FSAs with unsatisfiable constraints", () -> {
                final ImmutableList<StringSymbol> word1 = alphabetEncoding.translateBack(1, 3);
                encoding.ensureAcceptingWord(word1);
                encoding.ensureNotAcceptingWord(word1);
                expect(solver.findItSatisfiable()).toEqual(Boolean.FALSE);
            });

        });
    }
}
