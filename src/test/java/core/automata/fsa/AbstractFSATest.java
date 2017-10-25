package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

public abstract class AbstractFSATest
{
    protected final Alphabet<Object> alphabet;
    protected FSA.Provider provider;

    {
        final Object e = new Object();
        final Object a1 = new Object();
        final Object a2 = new Object();
        alphabet = Alphabets.builder(3, e).add(a1).add(a2).build();
        final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
        final ImmutableList<Object> word2 = Lists.immutable.of(a2, a2);
        final ImmutableList<Object> word3 = Lists.immutable.of(a2, a1);
        final ImmutableList<Object> word4 = Lists.immutable.of(a2, a2, a2, a2);

        describe("#incompleteStates", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> instance = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(instance::incompleteStates).toThrow(UnsupportedOperationException.class);
            });

            it("meets a minimum expectation", () -> {
                FSA<Object> instance = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(instance.incompleteStates().size()).toEqual(4);
                instance = provider.thatAcceptsOnly(alphabet, word4);
                expect(instance.incompleteStates().containsAllIterable(instance.states())).toBeTrue();
            });

        });

        describe("#accepts", () -> {

            it("returns false on invalid words", () -> {
                final FSA<Object> instance = provider.thatAcceptsOnly(alphabet, word1);
                expect(instance.accepts(Lists.immutable.of(new Object()))).toBeFalse();
                expect(instance.accepts(Lists.immutable.of(new Object(), null))).toBeFalse();
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfsa = provider.thatAcceptsOnly(alphabet, word1);
                expect(dfsa.isDeterministic()).toBeTrue();
                expect(dfsa.accepts(word1)).toBeTrue();
                expect(dfsa.accepts(word2)).toBeFalse();
                expect(dfsa.accepts(word3)).toBeFalse();
                expect(dfsa.accepts(word4)).toBeFalse();

                final FSA<Object> nfsa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(nfsa.isDeterministic()).toBeFalse();
                expect(nfsa.accepts(word1)).toBeFalse();
                expect(nfsa.accepts(word2)).toBeTrue();
                expect(nfsa.accepts(word3)).toBeTrue();
                expect(nfsa.accepts(word4)).toBeFalse();
            });

        });

        describe("#enumerateOneShortestWord", () -> {

            it("returns null on empty", () -> {
                expect(provider.thatAcceptsNone(alphabet).enumerateOneShortestWord()).toBeNull();
            });

            it("returns empty on accepting-all", () -> {
                expect(provider.thatAcceptsAll(alphabet).enumerateOneShortestWord().isEmpty()).toBeTrue();
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, word1);
                expect(fsa1.enumerateOneShortestWord()).toEqual(word1);
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, word2);
                expect(fsa2.enumerateOneShortestWord()).toEqual(word2);
                final FSA<Object> fsa3 = provider.thatAcceptsOnly(alphabet, word3);
                expect(fsa3.enumerateOneShortestWord()).toEqual(word3);
                final FSA<Object> fsa4 = provider.thatAcceptsOnly(alphabet, word4);
                expect(fsa4.enumerateOneShortestWord()).toEqual(word4);
                final FSA<Object> fsa5 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word4));
                expect(fsa5.enumerateOneShortestWord()).toEqual(word1);
                final FSA<Object> fsa6 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word4));
                expect(fsa6.enumerateOneShortestWord()).toEqual(word2);
                final FSA<Object> fsa7 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word3, word4));
                expect(fsa7.enumerateOneShortestWord()).toEqual(word3);
            });

        });
    }
}
