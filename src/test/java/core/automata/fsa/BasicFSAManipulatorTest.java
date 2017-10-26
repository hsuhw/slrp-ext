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
        final State s4 = States.generate();

        describe("#getDecoratee", () -> {

            it("returns the decoratee", () -> {
                final FSAManipulator decoratee = manipulator;
                final FSAManipulator.Decorator instance = new BasicFSAManipulator(decoratee);
                expect(instance.getDecoratee() == decoratee).toBeTrue();
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

        describe("#trimStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA.Builder<Object> builder = provider.builder(3, 3, e);
                builder.addTransition(s1, s2, a1).addTransition(s2, s3, a1).addTransition(s3, s4, a1);
                builder.addStartState(s2).addAcceptState(s3);
                final ImmutableSet<State> states = manipulator.trimDeadEndStates(builder.build()).states();
                expect(states.containsAllArguments(s2, s3)).toBeTrue();
                expect(states.containsAllArguments(s1, s4)).toBeFalse();
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
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(fsa.accepts(word1)).toBeTrue();
                expect(fsa.accepts(word2)).toBeTrue();
                expect(fsa.accepts(word3)).toBeFalse();
                expect(fsa.accepts(word4)).toBeFalse();
                expect(fsa.isDeterministic()).toBeFalse();
                final FSA<Object> dfsa = manipulator.determinize(fsa);
                expect(dfsa.accepts(word1)).toBeTrue();
                expect(dfsa.accepts(word2)).toBeTrue();
                expect(dfsa.accepts(word3)).toBeFalse();
                expect(dfsa.accepts(word4)).toBeFalse();
                expect(dfsa.isDeterministic()).toBeTrue();
            });

        });

        describe("#makeComplete", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(() -> manipulator.makeComplete(fsa)).toThrow(IllegalArgumentException.class);
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, word1);
                expect(fsa.accepts(word1)).toBeTrue();
                expect(fsa.accepts(word2)).toBeFalse();
                expect(fsa.isComplete()).toBeFalse();
                final FSA<Object> complete = manipulator.makeComplete(fsa);
                expect(complete.accepts(word1)).toBeTrue();
                expect(complete.accepts(word2)).toBeFalse();
                expect(complete.isComplete()).toBeTrue();
            });

        });

        describe("#makeComplement", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa = provider.thatAcceptsOnly(alphabet, word1);
                expect(fsa.accepts(word1)).toBeTrue();
                expect(fsa.accepts(word2)).toBeFalse();
                final FSA<Object> complement = manipulator.makeComplement(fsa);
                expect(complement.accepts(word1)).toBeFalse();
                expect(complement.accepts(word2)).toBeTrue();
            });

        });

        describe("#makeIntersection", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                final FSA<Object> intersection = manipulator.makeIntersection(fsa1, fsa2);
                expect(intersection.accepts(word1)).toBeFalse();
                expect(intersection.accepts(word2)).toBeTrue();
                expect(intersection.accepts(word3)).toBeFalse();
                expect(intersection.accepts(word4)).toBeFalse();
            });

        });

        describe("#makeUnion", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word1, word2));
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                final FSA<Object> union = manipulator.makeUnion(fsa1, fsa2);
                expect(union.accepts(word1)).toBeTrue();
                expect(union.accepts(word2)).toBeTrue();
                expect(union.accepts(word3)).toBeTrue();
                expect(union.accepts(word4)).toBeFalse();
            });

        });

        describe("#checkAcceptingNone", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, word1);
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, word2);
                final FSA<Object> none = provider.thatAcceptsNone(alphabet);
                expect(manipulator.checkAcceptingNone(fsa1)).toBeFalse();
                expect(manipulator.checkAcceptingNone(fsa2)).toBeFalse();
                expect(manipulator.checkAcceptingNone(none)).toBeTrue();
            });

        });

        describe("#checkAcceptingAll", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, word1);
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, word2);
                final FSA<Object> all = provider.thatAcceptsAll(alphabet);
                expect(manipulator.checkAcceptingAll(fsa1)).toBeFalse();
                expect(manipulator.checkAcceptingAll(fsa2)).toBeFalse();
                expect(manipulator.checkAcceptingAll(all)).toBeTrue();
            });

        });

        describe("#checkLanguageSubset", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = provider.thatAcceptsOnly(alphabet, word1);
                final FSA<Object> fsa2 = provider.thatAcceptsOnly(alphabet, word2);
                final FSA<Object> fsa3 = provider.thatAcceptsOnly(alphabet, Sets.immutable.of(word2, word3));
                final FSA<Object> none = provider.thatAcceptsNone(alphabet);
                final FSA<Object> all = provider.thatAcceptsAll(alphabet);
                expect(manipulator.checkLanguageSubset(fsa1, none)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa1, fsa1)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa1, fsa2)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa1, fsa3)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa1, all)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa2, none)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa2, fsa1)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa2, fsa2)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa2, fsa3)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa2, all)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa3, none)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa3, fsa1)).toBeFalse();
                expect(manipulator.checkLanguageSubset(fsa3, fsa2)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa3, fsa3)).toBeTrue();
                expect(manipulator.checkLanguageSubset(fsa3, all)).toBeFalse();
                expect(manipulator.checkLanguageSubset(none, none)).toBeTrue();
                expect(manipulator.checkLanguageSubset(none, fsa1)).toBeFalse();
                expect(manipulator.checkLanguageSubset(none, fsa2)).toBeFalse();
                expect(manipulator.checkLanguageSubset(none, fsa3)).toBeFalse();
                expect(manipulator.checkLanguageSubset(none, all)).toBeFalse();
                expect(manipulator.checkLanguageSubset(all, none)).toBeTrue();
                expect(manipulator.checkLanguageSubset(all, fsa1)).toBeTrue();
                expect(manipulator.checkLanguageSubset(all, fsa2)).toBeTrue();
                expect(manipulator.checkLanguageSubset(all, fsa3)).toBeTrue();
                expect(manipulator.checkLanguageSubset(all, all)).toBeTrue();
            });

        });
    }
}
