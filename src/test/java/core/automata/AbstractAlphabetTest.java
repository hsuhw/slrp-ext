package core.automata;

import api.automata.Alphabet;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

public abstract class AbstractAlphabetTest
{
    protected Alphabet.Provider Alphabets;

    {
        final Object e = new Object();
        final Object a1 = new Object();
        final Object a2 = new Object();
        final Twin<Object> pe = Tuples.twin(e, e);
        final Twin<Object> p1 = Tuples.twin(a1, a1);
        final Twin<Object> p2 = Tuples.twin(a1, a2);
        final Twin<Object> p3 = Tuples.twin(a2, a1);
        final Twin<Object> p4 = Tuples.twin(a2, a2);
        final ImmutableList<Object> worde = Lists.immutable.of(e);
        final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
        final ImmutableList<Object> word2 = Lists.immutable.of(a2, a2);
        final ImmutableList<Object> word3 = Lists.immutable.of(a1, a1, a1);

        describe("Making product", () -> {

            it("meets a minimum expectation", () -> {
                final Alphabet<Object> alphabet = Alphabets.builder(3, e).add(a1).add(a2).build();
                final Alphabet<Twin<Object>> product = Alphabets.product(alphabet);
                expect(product.size()).toEqual(5);
                expect(product.set().containsAllArguments(p1, p2, p3, p4)).toBeTrue();
            });

        });

        describe("Making twin word", () -> {

            it("complains on size-unmatched word pair", () -> {
                expect(() -> Alphabets.twinWord(worde, word1)).toThrow(IllegalArgumentException.class);
                expect(() -> Alphabets.twinWord(word2, word3)).toThrow(IllegalArgumentException.class);
            });

            it("meets a minimum expectation", () -> {
                final ImmutableList<Twin<Object>> pworde = Alphabets.twinWord(worde, worde);
                final ImmutableList<Twin<Object>> pword1 = Alphabets.twinWord(word1, word2);
                final ImmutableList<Twin<Object>> pword2 = Alphabets.twinWord(word2, word1);
                final ImmutableList<Twin<Object>> pword3 = Alphabets.twinWord(word3, word3);
                expect(pworde).toEqual(Lists.immutable.of(pe));
                expect(pword1).toEqual(Lists.immutable.of(p2, p4));
                expect(pword2).toEqual(Lists.immutable.of(p3, p4));
                expect(pword3).toEqual(Lists.immutable.of(p1, p1, p1));
            });

        });
    }
}
