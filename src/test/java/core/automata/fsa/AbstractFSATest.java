package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.States;
import api.automata.fsa.FSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

import java.util.Set;

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

        describe("#nonStart/AcceptStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(dfa.nonStartStates().size()).toEqual(3); // should be cached
                expect(dfa.nonAcceptStates().size()).toEqual(3); // should be cached
                Set intersect = Sets.intersect(dfa.nonStartStates().castToSet(), dfa.nonAcceptStates().castToSet());
                expect(intersect.size()).toEqual(2);

                final FSA<Object> nfa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(nfa.nonStartStates().size()).toEqual(3); // should be cached
                expect(nfa.nonAcceptStates().size()).toEqual(3); // should be cached
                intersect = Sets.intersect(nfa.nonStartStates().castToSet(), nfa.nonAcceptStates().castToSet());
                expect(intersect.size()).toEqual(2);
            });

        });

        final State s1 = States.generate();
        final State s2 = States.generate();
        final State s3 = States.generate();
        final State s4 = States.generate();
        final State s5 = States.generate();
        final State s6 = States.generate();
        final State s7 = States.generate();

        describe("#unreachableStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(7, 2, e);
                builder.addTransition(s1, s2, a1).addStartState(s2).addTransition(s2, s3, a1);
                final FSA<Object> fsa = builder.build();
                expect(fsa.unreachableStates().size()).toEqual(1);
                expect(fsa.unreachableStates().contains(s1)).toBeTrue();

                builder.addStartState(s4).addAcceptState(s5).addTransition(s6, s7, a1);
                final FSA<Object> multiGraph = builder.build();
                expect(multiGraph.unreachableStates().size()).toEqual(4);
                expect(multiGraph.unreachableStates().containsAllArguments(s1, s5, s6, s7)).toBeTrue();
            });

        });

        describe("#deadEndStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(7, 2, e);
                builder.addStartState(s1).addTransition(s1, s2, a1).addAcceptState(s2).addTransition(s2, s3, a1);
                final FSA<Object> singleGraph = builder.build();
                expect(singleGraph.deadEndStates().size()).toEqual(1);
                expect(singleGraph.deadEndStates().contains(s3)).toBeTrue();

                builder.addStartState(s4).addAcceptState(s5).addTransition(s6, s7, a1);
                final FSA<Object> multiGraph = builder.build();
                expect(multiGraph.deadEndStates().size()).toEqual(4);
                expect(multiGraph.deadEndStates().containsAllArguments(s3, s4, s6, s7)).toBeTrue();
            });

        });

        describe("#danglingStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(5, 2, e);
                builder.addTransition(s1, s2, a1).addStartState(s2).addTransition(s2, s2, a1);
                builder.addAcceptState(s2).addTransition(s2, s3, a1);
                final FSA<Object> fsa = builder.build();
                expect(fsa.danglingStates().size()).toEqual(2);
                expect(fsa.danglingStates().containsAllArguments(s1, s3)).toBeTrue();

                builder.addStartState(s4).addAcceptState(s5).addTransition(s6, s7, a1);
                final FSA<Object> multiGraph = builder.build();
                expect(multiGraph.danglingStates().size()).toEqual(6);
                expect(multiGraph.danglingStates().containsAllArguments(s1, s3, s4, s5, s6, s7)).toBeTrue();
            });

        });

        describe("#incompleteStates", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(fsa::incompleteStates).toThrow(UnsupportedOperationException.class);
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(dfa.incompleteStates().containsAllIterable(dfa.nonStartStates())).toBeTrue();
                expect(dfa.incompleteStates().size()).toEqual(3); // should be cached

                final FSA<Object> nfa = provider.thatAcceptsOnly(alphabet, word4);
                expect(nfa.incompleteStates().containsAllIterable(nfa.states())).toBeTrue();
                expect(nfa.incompleteStates().size()).toEqual(5); // should be cached
            });

        });

        describe("#accepts", () -> {

            it("returns false on invalid words", () -> {
                final FSA<Object> instance = provider.thatAcceptsOnly(alphabet, word1);
                expect(instance.accepts(Lists.immutable.of(new Object()))).toBeFalse();
                expect(instance.accepts(Lists.immutable.of(new Object(), null))).toBeFalse();
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfa = provider.thatAcceptsOnly(alphabet, word1);
                expect(dfa.isDeterministic()).toBeTrue();
                expect(dfa.accepts(word1)).toBeTrue();
                expect(dfa.accepts(word2)).toBeFalse();
                expect(dfa.accepts(word3)).toBeFalse();
                expect(dfa.accepts(word4)).toBeFalse();

                final FSA<Object> nfa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(nfa.isDeterministic()).toBeFalse();
                expect(nfa.accepts(word1)).toBeFalse();
                expect(nfa.accepts(word2)).toBeTrue();
                expect(nfa.accepts(word3)).toBeTrue();
                expect(nfa.accepts(word4)).toBeFalse();
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
