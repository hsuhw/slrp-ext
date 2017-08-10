package core.synth;

import api.automata.IntAlphabetTranslator;
import api.automata.fsa.FSA;
import api.synth.SatSolver;
import com.mscharhag.oleaster.runner.OleasterRunner;
import core.automata.StringSymbol;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

@RunWith(OleasterRunner.class)
public class BasicFSAEncodingTest
{
    private final SatSolver solver = new Sat4jSolverAdapter();
    private final IntAlphabetTranslator<StringSymbol> alphabetEncoding;
    private BasicFSAEncoding<StringSymbol> encoding;

    {
        // prepare alphabet encoding
        final StringSymbol[] symbols = new StringSymbol[5];
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = new StringSymbol("a" + i);
        }
        final ImmutableList<StringSymbol> definition = Lists.immutable.of(symbols);
        alphabetEncoding = new core.automata.IntAlphabetTranslator<>(definition, symbols[0]);

        describe("When determinism, no-dangling-states, no-dead-end-states all ensured", () -> {

            beforeEach(() -> {
                solver.reset();
                encoding = new BasicFSAEncoding<>(solver, 2, alphabetEncoding);
                encoding.ensureDeterminism();
                encoding.ensureNoDanglingStates();
                encoding.ensureNoDeadEndStates();
            });

            it("should produce correct FSA", () -> {
                final ImmutableIntList word1 = IntLists.immutable.of(1, 2, 3);
                final ImmutableIntList word2 = IntLists.immutable.of(2, 2, 3);
                final ImmutableIntList word3 = IntLists.immutable.of(3, 2, 3);
                encoding.ensureAcceptingWord(alphabetEncoding.translateBack(word1));
                encoding.ensureAcceptingWord(alphabetEncoding.translateBack(word2));
                encoding.ensureNotAcceptingWord(alphabetEncoding.translateBack(word3));

                final FSA<StringSymbol> solution = encoding.toFSA();
                expect(solution.accepts(alphabetEncoding.translateBack(word1))).toBeTrue();
                expect(solution.accepts(alphabetEncoding.translateBack(word2))).toBeTrue();
                expect(solution.accepts(alphabetEncoding.translateBack(word3))).toBeFalse();

                encoding.blockCurrentSolution();
                expect(encoding.toFSA().accepts(alphabetEncoding.translateBack(word3))).toBeFalse();
                encoding.blockCurrentSolution();
                expect(encoding.toFSA().accepts(alphabetEncoding.translateBack(word3))).toBeFalse();
                encoding.blockCurrentSolution();
                expect(encoding.toFSA().accepts(alphabetEncoding.translateBack(word3))).toBeFalse();
                encoding.blockCurrentSolution();
                expect(encoding.toFSA().accepts(alphabetEncoding.translateBack(word3))).toBeFalse();
            });

        });
    }
}
