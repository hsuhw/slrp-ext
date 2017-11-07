package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.States;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAManipulator;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

@RunWith(OleasterRunner.class)
public class BasicFSAManipulatorTest
{
    private final FSA.Provider provider = new BasicFSAs();
    private final FSAManipulator manipulator = new BasicFSAManipulator(null);
    private final Alphabet<Object> alphabet;

    {
        final Object e = new Object();
        final Object a1 = new Object();
        final Object a2 = new Object();
        final State s1 = States.generate();
        final State s2 = States.generate();
        final State s3 = States.generate();

        describe("#decoratee", () -> {

            it("returns the decoratee", () -> {
                final FSAManipulator decoratee = manipulator;
                final FSAManipulator.Decorator instance = new BasicFSAManipulator(decoratee);
                expect(instance.decoratee() == decoratee).toBeTrue();
            });

        });

        describe("#trimUnreachableStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(3, 3, e);
                builder.addTransition(s1, s2, a1).addStartState(s2).addTransition(s2, s3, a1);
                final ImmutableSet<State> states = manipulator.trimUnreachableStates(builder.build()).states();
                expect(states.contains(s1)).toBeFalse();
                expect(states.containsAllArguments(s2, s3)).toBeTrue();
            });

        });

        describe("#trimDeadEndStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(3, 3, e);
                builder.addTransition(s1, s2, a1).addStartState(s1).addAcceptState(s2).addTransition(s2, s3, a1);
                final ImmutableSet<State> states = manipulator.trimDeadEndStates(builder.build()).states();
                expect(states.containsAllArguments(s1, s2)).toBeTrue();
                expect(states.contains(s3)).toBeFalse();
            });

        });

        describe("#trimDanglingStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(3, 3, e);
                builder.addTransition(s1, s2, a1).addTransition(s2, s2, a1).addTransition(s2, s3, a1);
                builder.addStartState(s2).addAcceptState(s2);
                final ImmutableSet<State> states = manipulator.trimDanglingStates(builder.build()).states();
                expect(states.size()).toEqual(1);
                expect(states.contains(s2)).toBeTrue();
            });

        });

        alphabet = Alphabets.builder(3, e).add(a1).add(a2).build();
        final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
        final ImmutableList<Object> word2 = Lists.immutable.of(a1, a1);
        final ImmutableList<Object> word3 = Lists.immutable.of(a1, a2, a1);
        final ImmutableList<Object> word4 = Lists.immutable.of(a2, a1, a2);

        describe("#project", () -> {

            final Twin<Object> pe = Tuples.twin(e, e);
            final Twin<Object> p1 = Tuples.twin(a1, a1);
            final Twin<Object> p2 = Tuples.twin(a1, a2);
            final Twin<Object> p3 = Tuples.twin(a2, a1);
            final Twin<Object> p4 = Tuples.twin(a2, a2);
            final Alphabet<Twin<Object>> alph = Alphabets.builder(5, pe) //
                                                         .add(p1).add(p2).add(p3).add(p4).build();

            it("meets a minimum expectation", () -> {
                final ImmutableList<Twin<Object>> pword1 = Lists.immutable.of(p1, p2);
                final ImmutableList<Twin<Object>> pword2 = Lists.immutable.of(p3, p2, p1);
                final FSA<Twin<Object>> tfsa = provider.thatAcceptsOnly(alph, Sets.immutable.of(pword1, pword2));
                final FSA<Object> fsa = provider.manipulator().project(tfsa, alphabet, Twin::getTwo);
                expect(fsa.accepts(word1)).toBeTrue();
                expect(fsa.accepts(word2)).toBeFalse();
                expect(fsa.accepts(word3)).toBeTrue();
                expect(fsa.accepts(word4)).toBeFalse();
            });

        });

        describe("#determinize", () -> {

            it("does nothing on deterministic instances", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, word1);
                expect(manipulator.determinize(fsa) == fsa).toBeTrue();
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> nfa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(nfa.accepts(word1)).toBeTrue();
                expect(nfa.accepts(word2)).toBeTrue();
                expect(nfa.accepts(word3)).toBeFalse();
                expect(nfa.accepts(word4)).toBeFalse();
                expect(nfa.isDeterministic()).toBeFalse();
                final FSA<Object> dfa = manipulator.determinize(nfa);
                expect(dfa.accepts(word1)).toBeTrue();
                expect(dfa.accepts(word2)).toBeTrue();
                expect(dfa.accepts(word3)).toBeFalse();
                expect(dfa.accepts(word4)).toBeFalse();
                expect(dfa.isDeterministic()).toBeTrue();
            });

        });

        describe("#complete", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(() -> manipulator.complete(fsa)).toThrow(IllegalArgumentException.class);
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, word1);
                expect(fsa.accepts(word1)).toBeTrue();
                expect(fsa.accepts(word2)).toBeFalse();
                expect(fsa.isComplete()).toBeFalse();
                final FSA<Object> complete = manipulator.complete(fsa);
                expect(complete.accepts(word1)).toBeTrue();
                expect(complete.accepts(word2)).toBeFalse();
                expect(complete.isComplete()).toBeTrue();
            });

        });

        describe("#complement", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, word1);
                expect(fsa.accepts(word1)).toBeTrue();
                expect(fsa.accepts(word2)).toBeFalse();
                final FSA<Object> complement = manipulator.complement(fsa);
                expect(complement.accepts(word1)).toBeFalse();
                expect(complement.accepts(word2)).toBeTrue();
            });

        });

        describe("#intersect", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                final FSA<Object> intersection = manipulator.intersect(fsa1, fsa2);
                expect(intersection.accepts(word1)).toBeFalse();
                expect(intersection.accepts(word2)).toBeTrue();
                expect(intersection.accepts(word3)).toBeFalse();
                expect(intersection.accepts(word4)).toBeFalse();
            });

        });

        describe("#union", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                final FSA<Object> union = manipulator.union(fsa1, fsa2);
                expect(union.accepts(word1)).toBeTrue();
                expect(union.accepts(word2)).toBeTrue();
                expect(union.accepts(word3)).toBeTrue();
                expect(union.accepts(word4)).toBeFalse();
            });

        });

        describe("#checkSubset", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, word1);
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, word2);
                final FSA<Object> fsa3 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                final FSA<Object> none = provider.thatAcceptsNone(alphabet);
                final FSA<Object> all = provider.thatAcceptsAll(alphabet);
                expect(manipulator.checkSubset(none, fsa1).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa1, fsa1).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa2, fsa1).passed()).toBeFalse();
                expect(manipulator.checkSubset(fsa3, fsa1).passed()).toBeFalse();
                expect(manipulator.checkSubset(all, fsa1).passed()).toBeFalse();
                expect(manipulator.checkSubset(none, fsa2).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa1, fsa2).passed()).toBeFalse();
                expect(manipulator.checkSubset(fsa2, fsa2).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa3, fsa2).passed()).toBeFalse();
                expect(manipulator.checkSubset(all, fsa2).passed()).toBeFalse();
                expect(manipulator.checkSubset(none, fsa3).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa1, fsa3).passed()).toBeFalse();
                expect(manipulator.checkSubset(fsa2, fsa3).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa3, fsa3).passed()).toBeTrue();
                expect(manipulator.checkSubset(all, fsa3).passed()).toBeFalse();
                expect(manipulator.checkSubset(none, none).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa1, none).passed()).toBeFalse();
                expect(manipulator.checkSubset(fsa2, none).passed()).toBeFalse();
                expect(manipulator.checkSubset(fsa3, none).passed()).toBeFalse();
                expect(manipulator.checkSubset(all, none).passed()).toBeFalse();
                expect(manipulator.checkSubset(none, all).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa1, all).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa2, all).passed()).toBeTrue();
                expect(manipulator.checkSubset(fsa3, all).passed()).toBeTrue();
                expect(manipulator.checkSubset(all, all).passed()).toBeTrue();
            });

        });
    }
}
