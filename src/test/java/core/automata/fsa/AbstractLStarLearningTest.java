package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSAs;
import api.automata.fsa.LStarLearning;
import api.automata.fsa.LStarLearnings;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

public abstract class AbstractLStarLearningTest
{
    protected final Alphabet<Object> alphabet;
    protected final LStarLearning algorithm;

    protected abstract LStarLearning getAlgorithm();

    {
        algorithm = getAlgorithm();
        final var e = new Object();
        final var a1 = new Object();
        final var a2 = new Object();
        alphabet = Alphabets.builder(3, e).add(a1).add(a2).build();
        final var word1 = Lists.immutable.of(a1, a2);
        final var word2 = Lists.immutable.of(a2, a2);
        final var word3 = Lists.immutable.of(a2, a1);
        final var word4 = Lists.immutable.of(a2, a2, a2, a2);

        describe("With a simple teacher", () -> {

            it("can learn both DFAs and NFAs", () -> {
                final var dfa1 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word1, word2));
                final var result1 = algorithm.learn(alphabet, LStarLearnings.teacher(dfa1));
                expect(result1.accepts(word1)).toBeTrue();
                expect(result1.accepts(word2)).toBeTrue();
                expect(result1.accepts(word3)).toBeFalse();
                expect(result1.accepts(word4)).toBeFalse();

                final var dfa2 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word1, word4));
                final var result2 = algorithm.learn(alphabet, LStarLearnings.teacher(dfa2));
                expect(result2.accepts(word1)).toBeTrue();
                expect(result2.accepts(word2)).toBeFalse();
                expect(result2.accepts(word3)).toBeFalse();
                expect(result2.accepts(word4)).toBeTrue();

                final var nfa1 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word3));
                final var result3 = algorithm.learn(alphabet, LStarLearnings.teacher(nfa1));
                expect(result3.accepts(word1)).toBeFalse();
                expect(result3.accepts(word2)).toBeTrue();
                expect(result3.accepts(word3)).toBeTrue();
                expect(result3.accepts(word4)).toBeFalse();

                final var nfa2 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word4));
                final var result4 = algorithm.learn(alphabet, LStarLearnings.teacher(nfa2));
                expect(result4.accepts(word1)).toBeFalse();
                expect(result4.accepts(word2)).toBeTrue();
                expect(result4.accepts(word3)).toBeFalse();
                expect(result4.accepts(word4)).toBeTrue();
            });

        });
    }
}
