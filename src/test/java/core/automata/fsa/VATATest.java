package core.automata.fsa;

import api.automata.Alphabets;
import api.automata.fsa.FSAs;
import api.automata.fsa.VATA;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

@RunWith(OleasterRunner.class)
public class VATATest
{
    {
        final var e = new Object();
        final var a1 = new Object();
        final var a2 = new Object();
        final var alphabet = Alphabets.builder(3, e).add(a1).add(a2).build();
        final var word1 = Lists.immutable.of(a1, a2);
        final var word2 = Lists.immutable.of(a2, a2);
        final var word3 = Lists.immutable.of(a2, a1);
        final var word4 = Lists.immutable.of(a2, a2, a2, a2);

        describe("#reduce", () -> {

            it("handles NFAs that can be reduced", () -> {
                final MutableBag<ListIterable<Object>> bags = Bags.mutable.empty();
                bags.add(word1);
                bags.add(word1);
                bags.add(word1);
                final var nfa = FSAs.acceptingOnly(alphabet, bags);
                final var reduced = VATA.reduce(nfa);
                expect(reduced.states().size()).toEqual(3);
                expect(reduced.accepts(word1)).toBeTrue();
                expect(reduced.accepts(word2)).toBeFalse();
                expect(reduced.accepts(word3)).toBeFalse();
                expect(reduced.accepts(word4)).toBeFalse();
            });

            it("handles NFAs that can not be reduced", () -> {
                final var nfa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word4));
                final var reduced = VATA.reduce(nfa);
                expect(reduced.states().size()).toEqual(5);
                expect(reduced.accepts(word1)).toBeFalse();
                expect(reduced.accepts(word2)).toBeFalse();
                expect(reduced.accepts(word3)).toBeFalse();
                expect(reduced.accepts(word4)).toBeTrue();
            });

            it("handles empty NFAs", () -> {
                final var nfa = FSAs.acceptingNone(alphabet);
                final var reduced = VATA.reduce(nfa);
                expect(reduced.states().size()).toEqual(1);
                expect(reduced.acceptsNone()).toBeTrue();
            });

            it("handles all NFAs", () -> {
                final var nfa = FSAs.acceptingAll(alphabet);
                final var reduced = VATA.reduce(nfa);
                expect(reduced.states().size()).toEqual(1);
                expect(reduced.complement().acceptsNone()).toBeTrue();
            });

        });

        describe("#checkInclusion", () -> {

            it("meets a minimum expectation", () -> {
                final var fsa1 = FSAs.acceptingOnly(alphabet, word1);
                final var fsa2 = FSAs.acceptingOnly(alphabet, word3);
                final var fsa3 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word3));
                final var none = FSAs.acceptingNone(alphabet);
                final var all = FSAs.acceptingAll(alphabet);
                expect(VATA.checkInclusion(none, fsa1)).toBeTrue();
                expect(VATA.checkInclusion(fsa1, fsa1)).toBeTrue();
                expect(VATA.checkInclusion(fsa2, fsa1)).toBeFalse();
                expect(VATA.checkInclusion(fsa3, fsa1)).toBeFalse();
                expect(VATA.checkInclusion(all, fsa1)).toBeFalse();
                expect(VATA.checkInclusion(none, fsa2)).toBeTrue();
                expect(VATA.checkInclusion(fsa1, fsa2)).toBeFalse();
                expect(VATA.checkInclusion(fsa2, fsa2)).toBeTrue();
                expect(VATA.checkInclusion(fsa3, fsa2)).toBeFalse();
                expect(VATA.checkInclusion(all, fsa2)).toBeFalse();
                expect(VATA.checkInclusion(none, fsa3)).toBeTrue();
                expect(VATA.checkInclusion(fsa1, fsa3)).toBeFalse();
                expect(VATA.checkInclusion(fsa2, fsa3)).toBeTrue();
                expect(VATA.checkInclusion(fsa3, fsa3)).toBeTrue();
                expect(VATA.checkInclusion(all, fsa3)).toBeFalse();
                expect(VATA.checkInclusion(none, none)).toBeTrue();
                expect(VATA.checkInclusion(fsa1, none)).toBeFalse();
                expect(VATA.checkInclusion(fsa2, none)).toBeFalse();
                expect(VATA.checkInclusion(fsa3, none)).toBeFalse();
                expect(VATA.checkInclusion(all, none)).toBeFalse();
                expect(VATA.checkInclusion(none, all)).toBeTrue();
                expect(VATA.checkInclusion(fsa1, all)).toBeTrue();
                expect(VATA.checkInclusion(fsa2, all)).toBeTrue();
                expect(VATA.checkInclusion(fsa3, all)).toBeTrue();
                expect(VATA.checkInclusion(all, all)).toBeTrue();
            });

        });
    }
}
