package core.automata;

import api.automata.Alphabets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.Alphabet.Builder;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

abstract class AbstractAlphabetTest
{
    abstract Builder<Object> newBuilder(int capacity, Object epsilon);

    {
        final var e = new Object();
        final var a1 = new Object();
        final var a2 = new Object();
        final var pe = Tuples.pair(e, e);
        final var p1 = Tuples.pair(a1, a1);
        final var p2 = Tuples.pair(a1, a2);
        final var p3 = Tuples.pair(a2, a1);
        final var p4 = Tuples.pair(a2, a2);
        final ListIterable<Object> worde = Lists.immutable.of(e);
        final ListIterable<Object> word1 = Lists.immutable.of(a1, a2);
        final ListIterable<Object> word2 = Lists.immutable.of(a2, a2);
        final ListIterable<Object> word3 = Lists.immutable.of(a1, a1, a1);

        describe("Making product", () -> {

            it("meets a minimum expectation", () -> {
                final var alphabet = newBuilder(3, e).add(a1).add(a2).build();
                final var product = Alphabets.product(alphabet, alphabet);
                expect(product.size()).toEqual(5);
                expect(product.asSet().containsAllArguments(p1, p2, p3, p4)).toBeTrue();
            });

        });

        describe("Making twin word", () -> {

            it("complains on size-unmatched word pair", () -> {
                expect(() -> Alphabets.pairWord(worde, word1)).toThrow(IllegalArgumentException.class);
                expect(() -> Alphabets.pairWord(word2, word3)).toThrow(IllegalArgumentException.class);
            });

            it("meets a minimum expectation", () -> {
                final var pworde = Alphabets.pairWord(worde, worde);
                final var pword1 = Alphabets.pairWord(word1, word2);
                final var pword2 = Alphabets.pairWord(word2, word1);
                final var pword3 = Alphabets.pairWord(word3, word3);
                expect(pworde).toEqual(Lists.immutable.of(pe));
                expect(pword1).toEqual(Lists.immutable.of(p2, p4));
                expect(pword2).toEqual(Lists.immutable.of(p3, p4));
                expect(pword3).toEqual(Lists.immutable.of(p1, p1, p1));
            });

        });
    }
}
