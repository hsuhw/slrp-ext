package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.MutableState;
import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.MutableFSA;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.Automaton.TransitionGraph;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractMutableFSATest
{
    protected final Alphabet<Object> alphabet;

    protected abstract MutableFSA<Object> newFSA(Alphabet<Object> alphabet, int stateCapacity);

    {
        final Object e = new Object();
        final Object a1 = new Object();
        final Object a2 = new Object();
        alphabet = Alphabets.builder(3, e).add(a1).add(a2).build();
        final ImmutableList<Object> word1 = Lists.immutable.of(a1, a2);
        final ImmutableList<Object> word2 = Lists.immutable.of(a2, a2);
        final ImmutableList<Object> word3 = Lists.immutable.of(a2, a1);
        final ImmutableList<Object> word4 = Lists.immutable.of(a2, a2, a2, a2);

        describe("#nonAcceptStates", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(dfa.nonAcceptStates().size()).toEqual(3); // should be cached

                final FSA<Object> nfa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(nfa.nonAcceptStates().size()).toEqual(3); // should be cached
            });

        });

        describe("#unreachableStates", () -> {

            it("meets a minimum expectation", () -> {
                final MutableFSA<Object> fsa = newFSA(alphabet, 7);
                final MutableState<Object> s1 = (MutableState<Object>) fsa.startState();
                final MutableState<Object> s2 = fsa.newState();
                final MutableState<Object> s3 = fsa.newState();
                fsa.addTransition(s1, s2, a1).setAsStart(s2).addTransition(s2, s3, a1);
                final SetIterable<State<Object>> unreachable1 = fsa.unreachableStates();
                expect(unreachable1.size()).toEqual(1);
                expect(unreachable1.contains(s1)).toBeTrue();

                final MutableState<Object> s4 = fsa.newState();
                final MutableState<Object> s5 = fsa.newState();
                final MutableState<Object> s6 = fsa.newState();
                final MutableState<Object> s7 = fsa.newState();
                fsa.setAsStart(s4).setAsAccept(s5).addTransition(s6, s7, a1);
                final SetIterable<State<Object>> unreachable2 = fsa.unreachableStates();
                expect(unreachable2.size()).toEqual(6);
                expect(unreachable2.containsAllArguments(s1, s2, s3, s5, s6, s7)).toBeTrue();
            });

        });

        describe("#deadEndStates", () -> {

            it("meets a minimum expectation", () -> {
                final MutableFSA<Object> fsa = newFSA(alphabet, 7);
                final MutableState<Object> s1 = (MutableState<Object>) fsa.startState();
                final MutableState<Object> s2 = fsa.newState();
                final MutableState<Object> s3 = fsa.newState();
                fsa.addTransition(s1, s2, a1).setAsAccept(s2).addTransition(s2, s3, a1);
                final SetIterable<State<Object>> deadEndSet1 = fsa.deadEndStates();
                expect(deadEndSet1.size()).toEqual(1);
                expect(deadEndSet1.contains(s3)).toBeTrue();

                final MutableState<Object> s4 = fsa.newState();
                final MutableState<Object> s5 = fsa.newState();
                final MutableState<Object> s6 = fsa.newState();
                final MutableState<Object> s7 = fsa.newState();
                fsa.setAsStart(s4).setAsAccept(s5).addTransition(s6, s7, a1);
                final SetIterable<State<Object>> deadEndSet2 = fsa.deadEndStates();
                expect(deadEndSet2.size()).toEqual(4);
                expect(deadEndSet2.containsAllArguments(s3, s4, s6, s7)).toBeTrue();
            });

        });

        describe("#danglingStates", () -> {

            it("meets a minimum expectation", () -> {
                final MutableFSA<Object> fsa = newFSA(alphabet, 7);
                final MutableState<Object> s1 = (MutableState<Object>) fsa.startState();
                final MutableState<Object> s2 = fsa.newState();
                final MutableState<Object> s3 = fsa.newState();
                fsa.addTransition(s1, s2, a1).setAsStart(s2).addTransition(s2, s2, a1);
                fsa.setAsAccept(s2).addTransition(s2, s3, a1);
                final SetIterable<State<Object>> danglingSet1 = fsa.danglingStates();
                expect(danglingSet1.size()).toEqual(2);
                expect(danglingSet1.containsAllArguments(s1, s3)).toBeTrue();

                final MutableState<Object> s4 = fsa.newState();
                final MutableState<Object> s5 = fsa.newState();
                final MutableState<Object> s6 = fsa.newState();
                final MutableState<Object> s7 = fsa.newState();
                fsa.setAsStart(s4).setAsAccept(s5).addTransition(s6, s7, a1);
                final SetIterable<State<Object>> danglingSet2 = fsa.danglingStates();
                expect(danglingSet2.size()).toEqual(7);
                expect(danglingSet2.containsAllIterable(fsa.states())).toBeTrue();
            });

        });

        describe("#incompleteStates", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> fsa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(fsa::incompleteStates).toThrow(UnsupportedOperationException.class);
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfa1 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word1, word2));
                expect(dfa1.states().size()).toEqual(4);
                expect(dfa1.incompleteStates().contains(dfa1.startState())).toBeFalse();
                expect(dfa1.incompleteStates().size()).toEqual(3); // should be cached

                final FSA<Object> dfa2 = FSAs.acceptingOnly(alphabet, word4);
                expect(dfa2.incompleteStates().containsAllIterable(dfa2.states())).toBeTrue();
                expect(dfa2.incompleteStates().size()).toEqual(5); // should be cached
            });

        });

        describe("#accepts", () -> {

            it("returns false on invalid words", () -> {
                final FSA<Object> instance = FSAs.acceptingOnly(alphabet, word1);
                expect(instance.accepts(Lists.immutable.of(new Object()))).toBeFalse();
                expect(instance.accepts(Lists.immutable.of(new Object(), null))).toBeFalse();
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> dfa = FSAs.acceptingOnly(alphabet, word1);
                expect(dfa.isDeterministic()).toBeTrue();
                expect(dfa.accepts(word1)).toBeTrue();
                expect(dfa.accepts(word2)).toBeFalse();
                expect(dfa.accepts(word3)).toBeFalse();
                expect(dfa.accepts(word4)).toBeFalse();

                final FSA<Object> nfa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word3));
                expect(nfa.isDeterministic()).toBeFalse();
                expect(nfa.accepts(word1)).toBeFalse();
                expect(nfa.accepts(word2)).toBeTrue();
                expect(nfa.accepts(word3)).toBeTrue();
                expect(nfa.accepts(word4)).toBeFalse();
            });

        });

        describe("#acceptsNone", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = FSAs.acceptingOnly(alphabet, word1);
                final FSA<Object> fsa2 = FSAs.acceptingOnly(alphabet, word2);
                final FSA<Object> none = FSAs.acceptingNone(alphabet);
                expect(fsa1.acceptsNone()).toBeFalse();
                expect(fsa2.acceptsNone()).toBeFalse();
                expect(none.acceptsNone()).toBeTrue();
            });

        });

        describe("#enumerateOneShortest", () -> {

            it("returns null on empty", () -> {
                expect(FSAs.acceptingNone(alphabet).enumerateOneShortest()).toBeNull();
            });

            it("returns empty on accepting-all", () -> {
                expect(FSAs.acceptingAll(alphabet).enumerateOneShortest().isEmpty()).toBeTrue();
            });

            fit("meets a minimum expectation", () -> {
                final FSA<Object> dfa1 = FSAs.acceptingOnly(alphabet, word1);
                expect(dfa1.enumerateOneShortest()).toEqual(word1);
                final FSA<Object> dfa2 = FSAs.acceptingOnly(alphabet, word2);
                expect(dfa2.enumerateOneShortest()).toEqual(word2);
                final FSA<Object> dfa3 = FSAs.acceptingOnly(alphabet, word3);
                expect(dfa3.enumerateOneShortest()).toEqual(word3);
                final FSA<Object> dfa4 = FSAs.acceptingOnly(alphabet, word4);
                expect(dfa4.enumerateOneShortest()).toEqual(word4);
                final FSA<Object> dfa5 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word1, word4));
                expect(dfa5.enumerateOneShortest()).toEqual(word1);
                final FSA<Object> nfa1 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word2, word4));
                expect(nfa1.enumerateOneShortest()).toEqual(word2);
                final FSA<Object> nfa2 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(word3, word4));
                expect(nfa2.enumerateOneShortest()).toEqual(word3);
            });

        });

        describe("#trimUnreachableStates", () -> {

            it("meets a minimum expectation", () -> {
                final MutableFSA<Object> fsa = newFSA(alphabet, 3);
                final MutableState<Object> s1 = (MutableState<Object>) fsa.startState();
                final MutableState<Object> s2 = fsa.newState();
                final MutableState<Object> s3 = fsa.newState();
                fsa.addTransition(s1, s2, a1).setAsStart(s2).addTransition(s2, s3, a1);
                expect(fsa.unreachableStates().size()).toBeGreaterThan(0);
                expect(fsa.trimUnreachableStates().unreachableStates().isEmpty()).toBeTrue();
            });

        });

        final ImmutableList<Object> input1 = Lists.immutable.of(a1, a2);
        final ImmutableList<Object> input2 = Lists.immutable.of(a1, a1);
        final ImmutableList<Object> input3 = Lists.immutable.of(a1, a2, a1);
        final ImmutableList<Object> input4 = Lists.immutable.of(a2, a1, a2);
        final ImmutableList<Object> input5 = Lists.immutable.of(a1, a2, a2, a2);
        final ImmutableList<Object> input6 = Lists.immutable.of(a1, a1, a1, a2, a2, a2);

        describe("#project", () -> {

            final Twin<Object> pe = Tuples.twin(e, e);
            final Twin<Object> p1 = Tuples.twin(a1, a1);
            final Twin<Object> p2 = Tuples.twin(a1, a2);
            final Twin<Object> p3 = Tuples.twin(a2, a1);
            final Twin<Object> p4 = Tuples.twin(a2, a2);
            final Alphabet.Builder<Twin<Object>> pBuilder = Alphabets.builder(5, pe);
            final Alphabet<Twin<Object>> pAlphabet = pBuilder.add(p1).add(p2).add(p3).add(p4).build();

            it("meets a minimum expectation", () -> {
                final ImmutableList<Twin<Object>> pInput1 = Lists.immutable.of(p1, p2);
                final ImmutableList<Twin<Object>> pInput2 = Lists.immutable.of(p3, p2, p1);
                final FSA<Twin<Object>> fixture = FSAs.acceptingOnly(pAlphabet, Sets.immutable.of(pInput1, pInput2));
                final FSA<Object> fsa = fixture.project(alphabet, Twin::getTwo);
                expect(fsa.accepts(input1)).toBeTrue();
                expect(fsa.accepts(input2)).toBeFalse();
                expect(fsa.accepts(input3)).toBeTrue();
                expect(fsa.accepts(input4)).toBeFalse();
            });

        });

        describe("#determinize", () -> {

            it("does nothing on deterministic instances", () -> {
                final FSA<Object> fsa = FSAs.acceptingOnly(alphabet, input1);
                expect(fsa.determinize() == fsa).toBeTrue();
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> nfa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input1, input2));
                expect(nfa.accepts(input1)).toBeTrue();
                expect(nfa.accepts(input2)).toBeTrue();
                expect(nfa.accepts(input3)).toBeFalse();
                expect(nfa.accepts(input4)).toBeFalse();
                expect(nfa.isDeterministic()).toBeFalse();
                final FSA<Object> dfa = nfa.determinize();
                expect(dfa.accepts(input1)).toBeTrue();
                expect(dfa.accepts(input2)).toBeTrue();
                expect(dfa.accepts(input3)).toBeFalse();
                expect(dfa.accepts(input4)).toBeFalse();
                expect(dfa.isDeterministic()).toBeTrue();
            });

        });

        describe("#minimize", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> fsa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input1, input2));
                expect(fsa::minimize).toThrow(UnsupportedOperationException.class);
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> nfa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input5, input6));
                final FSA<Object> dfa = nfa.determinize();
                expect(dfa.accepts(input1)).toBeFalse();
                expect(dfa.accepts(input2)).toBeFalse();
                expect(dfa.accepts(input3)).toBeFalse();
                expect(dfa.accepts(input4)).toBeFalse();
                expect(dfa.accepts(input5)).toBeTrue();
                expect(dfa.accepts(input6)).toBeTrue();
                final FSA<Object> min = dfa.minimize();
                expect(min.accepts(input1)).toBeFalse();
                expect(min.accepts(input2)).toBeFalse();
                expect(min.accepts(input3)).toBeFalse();
                expect(min.accepts(input4)).toBeFalse();
                expect(min.accepts(input5)).toBeTrue();
                expect(min.accepts(input6)).toBeTrue();
                expect(min.states().size()).toBeSmallerThan(dfa.states().size());
            });

        });

        describe("#complete", () -> {

            it("complains on nondeterministic instances", () -> {
                final FSA<Object> fsa = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input1, input2));
                expect(fsa::complete).toThrow(UnsupportedOperationException.class);
            });

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa = FSAs.acceptingOnly(alphabet, input1);
                expect(fsa.accepts(input1)).toBeTrue();
                expect(fsa.accepts(input2)).toBeFalse();
                expect(fsa.isComplete()).toBeFalse();
                final FSA<Object> complete = fsa.complete();
                expect(complete.accepts(input1)).toBeTrue();
                expect(complete.accepts(input2)).toBeFalse();
                expect(complete.isComplete()).toBeTrue();
            });

        });

        describe("#complement", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa = FSAs.acceptingOnly(alphabet, input1);
                expect(fsa.accepts(input1)).toBeTrue();
                expect(fsa.accepts(input2)).toBeFalse();
                final FSA<Object> complement = fsa.complement();
                expect(complement.accepts(input1)).toBeFalse();
                expect(complement.accepts(input2)).toBeTrue();
            });

        });

        describe("#intersect", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input1, input2));
                final FSA<Object> fsa2 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input2, input3));
                final FSA<Object> intersection = fsa1.intersect(fsa2);
                expect(intersection.accepts(input1)).toBeFalse();
                expect(intersection.accepts(input2)).toBeTrue();
                expect(intersection.accepts(input3)).toBeFalse();
                expect(intersection.accepts(input4)).toBeFalse();
            });

        });

        describe("#union", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input1, input2));
                final FSA<Object> fsa2 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input2, input3));
                final FSA<Object> union = fsa1.union(fsa2);
                expect(union.accepts(input1)).toBeTrue();
                expect(union.accepts(input2)).toBeTrue();
                expect(union.accepts(input3)).toBeTrue();
                expect(union.accepts(input4)).toBeFalse();
            });

        });

        describe("#checkContaining", () -> {

            it("meets a minimum expectation", () -> {
                final FSA<Object> fsa1 = FSAs.acceptingOnly(alphabet, input1);
                final FSA<Object> fsa2 = FSAs.acceptingOnly(alphabet, input2);
                final FSA<Object> fsa3 = FSAs.acceptingOnly(alphabet, Sets.immutable.of(input2, input3));
                final FSA<Object> none = FSAs.acceptingNone(alphabet);
                final FSA<Object> all = FSAs.acceptingAll(alphabet);
                expect(fsa1.checkContaining(none).passed()).toBeTrue();
                expect(fsa1.checkContaining(fsa1).passed()).toBeTrue();
                expect(fsa1.checkContaining(fsa2).passed()).toBeFalse();
                expect(fsa1.checkContaining(fsa2).counterexample().witness()).toEqual(input2);
                expect(fsa1.checkContaining(fsa3).passed()).toBeFalse();
                expect(fsa1.checkContaining(fsa3).counterexample().witness()).toEqual(input2);
                expect(fsa1.checkContaining(all).passed()).toBeFalse();
                expect(fsa2.checkContaining(none).passed()).toBeTrue();
                expect(fsa2.checkContaining(fsa1).passed()).toBeFalse();
                expect(fsa2.checkContaining(fsa1).counterexample().witness()).toEqual(input1);
                expect(fsa2.checkContaining(fsa2).passed()).toBeTrue();
                expect(fsa2.checkContaining(fsa3).passed()).toBeFalse();
                expect(fsa2.checkContaining(fsa3).counterexample().witness()).toEqual(input3);
                expect(fsa2.checkContaining(all).passed()).toBeFalse();
                expect(fsa3.checkContaining(none).passed()).toBeTrue();
                expect(fsa3.checkContaining(fsa1).passed()).toBeFalse();
                expect(fsa2.checkContaining(fsa1).counterexample().witness()).toEqual(input1);
                expect(fsa3.checkContaining(fsa2).passed()).toBeTrue();
                expect(fsa3.checkContaining(fsa3).passed()).toBeTrue();
                expect(fsa3.checkContaining(all).passed()).toBeFalse();
                expect(none.checkContaining(none).passed()).toBeTrue();
                expect(none.checkContaining(fsa1).passed()).toBeFalse();
                expect(none.checkContaining(fsa1).counterexample().witness()).toEqual(input1);
                expect(none.checkContaining(fsa2).passed()).toBeFalse();
                expect(none.checkContaining(fsa2).counterexample().witness()).toEqual(input2);
                expect(none.checkContaining(fsa3).passed()).toBeFalse();
                expect(none.checkContaining(fsa3).counterexample().witness()).toEqual(input2);
                expect(none.checkContaining(all).passed()).toBeFalse();
                expect(all.checkContaining(none).passed()).toBeTrue();
                expect(all.checkContaining(fsa1).passed()).toBeTrue();
                expect(all.checkContaining(fsa2).passed()).toBeTrue();
                expect(all.checkContaining(fsa3).passed()).toBeTrue();
                expect(all.checkContaining(all).passed()).toBeTrue();
            });

        });

        describe("#transitionGraph", () -> {

            describe("nondeterministic", () -> {

                final MutableFSA<Object> fsa = newFSA(alphabet, 3);
                final MutableState<Object> n0 = (MutableState<Object>) fsa.startState();
                final MutableState<Object> n1 = fsa.newState();
                final MutableState<Object> n2 = fsa.newState();
                final MutableState<Object> n3 = fsa.newState();
                fsa.addTransition(n0, n1, a1);
                fsa.addTransition(n1, n1, a1);
                fsa.addTransition(n1, n2, a1);
                fsa.addEpsilonTransition(n1, n3);
                fsa.addEpsilonTransition(n2, n1);
                final TransitionGraph<Object> graph = fsa.transitionGraph();

                describe("#size", () -> {

                    it("returns the number of its arcs", () -> {
                        expect(graph.size()).toEqual(5);
                        expect(graph.size()).toEqual(5); // should be cached
                    });

                });

                describe("#isEmpty", () -> {

                    it("meets a minimum expectation", () -> {
                        expect(newFSA(alphabet, 1).transitionGraph().isEmpty()).toBeTrue();
                        expect(graph.isEmpty()).toBeFalse();
                    });

                });

                describe("#referredNodes", () -> {

                    it("returns the nodes", () -> {
                        expect(graph.referredNodes().size()).toEqual(4);  // nodes should be cached
                        expect(graph.referredNodes().containsAllArguments(n0, n1, n2, n3)).toBeTrue();
                    });

                });

                describe("#referredArcLabels", () -> {

                    it("returns the labels", () -> {
                        expect(graph.referredArcLabels().size()).toEqual(2); // labels should be cached
                        expect(graph.referredArcLabels().containsAllArguments(e, a1)).toBeTrue();
                    });

                });

                describe("#epsilonLabel", () -> {

                    it("returns the epsilon label", () -> {
                        expect(graph.epsilonLabel()).toEqual(e);
                    });

                });

                describe("#enableArcsOn", () -> {

                    it("returns the arcs including epsilon", () -> {
                        final SetIterable<Object> n1Arcs = graph.arcLabelsFrom(n1);
                        final SetIterable<Object> n2Arcs = graph.arcLabelsFrom(n2);
                        expect(n1Arcs.size()).toEqual(2);
                        expect(n1Arcs.containsAllArguments(e, a1)).toBeTrue();
                        expect(n2Arcs.size()).toEqual(1);
                        expect(n2Arcs.contains(e)).toBeTrue();
                    });

                    it("returns empty if it's a dead end", () -> {
                        expect(graph.arcLabelsFrom(n3).isEmpty()).toBeTrue();
                    });

                });

                describe("#nonEpsilonArcLabelsFrom", () -> {

                    it("returns the non-epsilon arcs", () -> {
                        final SetIterable<Object> n1Arcs = graph.nonEpsilonArcLabelsFrom(n1);
                        expect(n1Arcs.size()).toEqual(1);
                        expect(n1Arcs.contains(a1)).toBeTrue();
                        expect(graph.nonEpsilonArcLabelsFrom(n2).isEmpty()).toBeTrue();
                    });

                    it("returns empty if it's a dead end", () -> {
                        expect(graph.nonEpsilonArcLabelsFrom(n3).isEmpty()).toBeTrue();
                    });

                });

                describe("#arcLabeledFrom", () -> {

                    it("returns whether an arc exists", () -> {
                        expect(graph.arcLabeledFrom(n1, e)).toBeTrue();
                        expect(graph.arcLabeledFrom(n1, a1)).toBeTrue();
                        expect(graph.arcLabeledFrom(n2, e)).toBeTrue();
                        expect(graph.arcLabeledFrom(n2, a1)).toBeFalse();
                        expect(graph.arcLabeledFrom(n3, e)).toBeFalse();
                        expect(graph.arcLabeledFrom(n3, a1)).toBeFalse();
                    });

                });

                describe("#directSuccessorsOf(node)", () -> {

                    it("returns the successors", () -> {
                        final SetIterable<State<Object>> succs1 = graph.directSuccessorsOf(n1);
                        final SetIterable<State<Object>> succs2 = graph.directSuccessorsOf(n2);
                        expect(succs1.size()).toEqual(3);
                        expect(succs1.containsAllArguments(n1, n2, n3)).toBeTrue();
                        expect(succs2.size()).toEqual(1);
                        expect(succs2.contains(n1)).toBeTrue();
                    });

                    it("returns empty if it's a dead end", () -> {
                        expect(graph.directSuccessorsOf(n3).isEmpty()).toBeTrue();
                    });

                });

                describe("#directSuccessorsOf(node, label)", () -> {

                    it("returns the successors", () -> {
                        final SetIterable<State<Object>> succs1 = graph.directSuccessorsOf(n1, a1);
                        final SetIterable<State<Object>> succs2 = graph.directSuccessorsOf(n2, e);
                        expect(succs1.size()).toEqual(2);
                        expect(succs1.containsAllArguments(n1, n2)).toBeTrue();
                        expect(succs2.size()).toEqual(1);
                        expect(succs2.contains(n1)).toBeTrue();
                    });

                    it("returns empty if it's a dead end", () -> {
                        expect(graph.directSuccessorsOf(n3, e).isEmpty()).toBeTrue();
                        expect(graph.directSuccessorsOf(n3, a1).isEmpty()).toBeTrue();
                    });

                });

                describe("#directSuccessorsOf(nodes, label)", () -> {

                    it("returns the successors", () -> {
                        final SetIterable<State<Object>> set1 = Sets.immutable.of(n1, n3);
                        final SetIterable<State<Object>> succs1 = graph.directSuccessorsOf(set1, a1);
                        final SetIterable<State<Object>> set2 = Sets.immutable.of(n1, n2);
                        final SetIterable<State<Object>> succs2 = graph.directSuccessorsOf(set2, e);
                        expect(succs1.size()).toEqual(2);
                        expect(succs1.containsAllArguments(n1, n2)).toBeTrue();
                        expect(succs2.size()).toEqual(2);
                        expect(succs2.containsAllArguments(n1, n3)).toBeTrue();
                    });

                    it("returns empty if it's a dead end singleton", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.of(n3);
                        expect(graph.directSuccessorsOf(set, e).isEmpty()).toBeTrue();
                        expect(graph.directSuccessorsOf(set, a1).isEmpty()).toBeTrue();
                    });

                    it("returns empty if an empty is given", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.empty();
                        expect(graph.directSuccessorsOf(set, e).isEmpty()).toBeTrue();
                        expect(graph.directSuccessorsOf(set, a1).isEmpty()).toBeTrue();
                    });

                });

                describe("#directSuccessorOf(node, label)", () -> {

                    it("complains with multiple successors", () -> {
                        expect(() -> graph.directSuccessorOf(n1, a1)).toThrow(IllegalStateException.class);
                    });

                });

                xdescribe("#directPredecessorsOf(node)", () -> {

                    it("returns the predecessors", () -> {
                        final SetIterable<State<Object>> preds1 = graph.directPredecessorsOf(n1);
                        final SetIterable<State<Object>> preds2 = graph.directPredecessorsOf(n2);
                        expect(preds1.size()).toEqual(3);
                        expect(preds1.containsAllArguments(n0, n1, n2)).toBeTrue();
                        expect(preds2.size()).toEqual(1);
                        expect(preds2.contains(n1)).toBeTrue();
                    });

                    it("returns empty if it's a start node", () -> {
                        expect(graph.directPredecessorsOf(n0).isEmpty()).toBeTrue();
                    });

                });

                xdescribe("#directPredecessorsOf(node, label)", () -> {

                    it("returns the predecessors", () -> {
                        final SetIterable<State<Object>> n1preds1 = graph.directPredecessorsOf(n1, e);
                        final SetIterable<State<Object>> n1preds2 = graph.directPredecessorsOf(n1, a1);
                        final SetIterable<State<Object>> n2preds1 = graph.directPredecessorsOf(n2, e);
                        final SetIterable<State<Object>> n2preds2 = graph.directPredecessorsOf(n2, a1);
                        expect(n1preds1.size()).toEqual(1);
                        expect(n1preds1.contains(n2)).toBeTrue();
                        expect(n1preds2.size()).toEqual(2);
                        expect(n1preds2.containsAllArguments(n0, n1)).toBeTrue();
                        expect(n2preds1.isEmpty()).toBeTrue();
                        expect(n2preds2.size()).toEqual(1);
                        expect(n2preds2.contains(n1)).toBeTrue();
                    });

                    it("returns empty if it's a start node", () -> {
                        expect(graph.directPredecessorsOf(n0, e).isEmpty()).toBeTrue();
                        expect(graph.directPredecessorsOf(n0, a1).isEmpty()).toBeTrue();
                    });

                });

                xdescribe("#directPredecessorsOf(nodes, label)", () -> {

                    it("returns the predecessors", () -> {
                        final SetIterable<State<Object>> set1 = Sets.immutable.of(n1, n3);
                        final SetIterable<State<Object>> preds1 = graph.directPredecessorsOf(set1, a1);
                        final SetIterable<State<Object>> set2 = Sets.immutable.of(n1, n2);
                        final SetIterable<State<Object>> preds2 = graph.directPredecessorsOf(set2, e);
                        expect(preds1.size()).toEqual(2);
                        expect(preds1.containsAllArguments(n0, n1)).toBeTrue();
                        expect(preds2.size()).toEqual(1);
                        expect(preds2.contains(n2)).toBeTrue();
                    });

                    it("returns empty if it's a dead end singleton", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.of(n0);
                        expect(graph.directPredecessorsOf(set, e).isEmpty()).toBeTrue();
                        expect(graph.directPredecessorsOf(set, a1).isEmpty()).toBeTrue();
                    });

                    it("returns empty if an empty is given", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.empty();
                        expect(graph.directPredecessorsOf(set, e).isEmpty()).toBeTrue();
                        expect(graph.directPredecessorsOf(set, a1).isEmpty()).toBeTrue();
                    });

                });

                describe("#epsilonClosureOf(nodes)", () -> {

                    it("returns the closure", () -> {
                        final SetIterable<State<Object>> set1 = Sets.immutable.of(n0);
                        final SetIterable<State<Object>> closure1 = graph.epsilonClosureOf(set1);
                        final SetIterable<State<Object>> set2 = Sets.immutable.of(n1);
                        final SetIterable<State<Object>> closure2 = graph.epsilonClosureOf(set2);
                        final SetIterable<State<Object>> set3 = Sets.immutable.of(n2);
                        final SetIterable<State<Object>> closure3 = graph.epsilonClosureOf(set3);
                        expect(closure1.size()).toEqual(1);
                        expect(closure1.contains(n0)).toBeTrue();
                        expect(closure2.size()).toEqual(2);
                        expect(closure2.containsAllArguments(n1, n3)).toBeTrue();
                        expect(closure3.size()).toEqual(3);
                        expect(closure3.containsAllArguments(n1, n2, n3)).toBeTrue();
                    });

                    it("returns empty if an empty is given", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.empty();
                        expect(graph.epsilonClosureOf(set).isEmpty()).toBeTrue();
                    });

                });

                describe("#epsilonClosureOf(nodes, label)", () -> {

                    it("returns the closure", () -> {
                        final SetIterable<State<Object>> set1 = Sets.immutable.of(n0);
                        final SetIterable<State<Object>> closure1 = graph.epsilonClosureOf(set1, a1);
                        final SetIterable<State<Object>> set2 = Sets.immutable.of(n1);
                        final SetIterable<State<Object>> closure2 = graph.epsilonClosureOf(set2, e);
                        final SetIterable<State<Object>> set3 = Sets.immutable.of(n2);
                        final SetIterable<State<Object>> closure3 = graph.epsilonClosureOf(set3, a1);
                        expect(closure1.size()).toEqual(2);
                        expect(closure1.containsAllArguments(n1, n3)).toBeTrue();
                        expect(closure2.size()).toEqual(2);
                        expect(closure2.containsAllArguments(n1, n3)).toBeTrue();
                        expect(closure3.size()).toEqual(3);
                        expect(closure3.containsAllArguments(n1, n2, n3)).toBeTrue();
                    });

                    it("returns empty if an empty is given", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.empty();
                        expect(graph.epsilonClosureOf(set, a1).isEmpty()).toBeTrue();
                    });

                });

            });

            describe("deterministic", () -> {

                final MutableFSA<Object> fsa = newFSA(alphabet, 3);
                final MutableState<Object> n0 = (MutableState<Object>) fsa.startState();
                final MutableState<Object> n1 = fsa.newState();
                final MutableState<Object> n2 = fsa.newState();
                fsa.addTransition(n0, n1, a1);
                fsa.addTransition(n1, n2, a1);
                final TransitionGraph<Object> graph = fsa.transitionGraph();

                describe("#epsilonClosureOf(nodes)", () -> {

                    it("complains", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.of(n0);
                        expect(() -> graph.epsilonClosureOf(set)).toThrow(UnsupportedOperationException.class);
                    });

                });

                describe("#epsilonClosureOf(nodes, label)", () -> {

                    it("complains", () -> {
                        final SetIterable<State<Object>> set = Sets.immutable.of(n0);
                        expect(() -> graph.epsilonClosureOf(set, a1)).toThrow(UnsupportedOperationException.class);
                    });

                });

            });

        });
    }
}
