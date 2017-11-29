package core.automata;

import api.automata.TransitionGraph;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;

import static api.automata.TransitionGraph.Builder;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractTransitionGraphTest
{
    protected final Object nullArg = null;
    protected final Object epsilon = new Object();
    protected Builder<Object, Object> builder;
    protected TransitionGraph<Object, Object> graph;

    protected abstract Builder<Object, Object> newBuilder();

    {
        final Object n0 = new Object();
        final Object n1 = new Object();
        final Object n2 = new Object();
        final Object n3 = new Object();
        final Object label = new Object();

        describe("Using the builder", () -> {

            beforeEach(() -> {
                builder = newBuilder();
            });

            describe("#currentSize", () -> {

                it("returns the added arc number", () -> {
                    expect(builder.currentSize()).toEqual(0);

                    builder.addArc(n0, n0, label);
                    builder.addArc(n0, n1, epsilon);
                    builder.addArc(n1, n0, label);
                    expect(builder.currentSize()).toEqual(3);

                    builder.addArc(n1, n0, epsilon);
                    expect(builder.currentSize()).toEqual(4);
                });

            });

            describe("#isEmpty", () -> {

                it("meets a minimum expectation", () -> {
                    expect(builder.isEmpty()).toBeTrue();

                    builder.addArc(n0, n0, label);
                    expect(builder.isEmpty()).toBeFalse();
                });

            });

            describe("#addArc", () -> {

                it("only accepts non-null arguments", () -> {
                    final Object o = new Object();
                    expect(() -> builder.addArc(nullArg, o, o)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.addArc(o, nullArg, o)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.addArc(o, o, nullArg)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.addArc(nullArg, nullArg, o)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.addArc(o, nullArg, nullArg)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.addArc(nullArg, o, nullArg)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.addArc(nullArg, nullArg, nullArg)).toThrow(IllegalArgumentException.class);
                });

                it("adds no epsilon self-loop", () -> {
                    builder.addArc(n0, n0, epsilon);
                    expect(builder.currentSize()).toEqual(0);
                });

                it("meets a minimum expectation", () -> {
                    builder.addArc(n1, n1, label);
                    builder.addArc(n1, n2, label);
                    expect(builder.currentSize()).toEqual(2);
                });

            });

            describe("#removeArc", () -> {

                it("only accepts non-null arguments", () -> {
                    final Object o = new Object();
                    expect(() -> builder.removeArc(nullArg, o, o)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.removeArc(o, nullArg, o)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.removeArc(o, o, nullArg)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.removeArc(nullArg, nullArg, o)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.removeArc(o, nullArg, nullArg)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.removeArc(nullArg, o, nullArg)).toThrow(IllegalArgumentException.class);
                    expect(() -> builder.removeArc(nullArg, nullArg, nullArg)).toThrow(IllegalArgumentException.class);
                });

                it("complains when removing non-existing arc", () -> {
                    expect(() -> builder.removeArc(n1, n2, label)).toThrow(Exception.class);

                    builder.addArc(n1, n1, label);
                    expect(() -> builder.removeArc(n1, n2, label)).toThrow(Exception.class);
                });

                it("meets a minimum expectation", () -> {
                    builder.addArc(n1, n1, label);
                    builder.addArc(n1, n2, label);
                    builder.removeArc(n1, n2, label);
                    expect(builder.currentSize()).toEqual(1);

                    builder.removeArc(n1, n1, label);
                    expect(builder.currentSize()).toEqual(0);
                });

            });

            describe("#removeNode", () -> {

                it("only accepts non-null argument", () -> {
                    expect(() -> builder.removeNode(nullArg)).toThrow(IllegalArgumentException.class);
                });

                it("removes forward-only nodes", () -> {
                    builder.addArc(n1, n2, label);
                    builder.removeNode(n1);
                    expect(builder.currentSize()).toEqual(0);
                });

                it("removes backward-only nodes", () -> {
                    builder.addArc(n1, n2, label);
                    builder.removeNode(n2);
                    expect(builder.currentSize()).toEqual(0);
                });

                it("removes for/backward-removing nodes", () -> {
                    builder.addArc(n1, n2, label);
                    builder.addArc(n2, n1, label);
                    builder.removeNode(n1);
                    expect(builder.currentSize()).toEqual(0);
                });

            });

            describe("#build", () -> {

                it("meets a minimum expectation", () -> {
                    expect(builder.build().isEmpty()).toBeTrue();

                    builder.addArc(n0, n0, label);
                    expect(builder.build().isEmpty()).toBeFalse();
                });

            });

        });

        describe("#arcDeterministic", () -> {

            it("returns true on empty", () -> {
                expect(newBuilder().build().arcDeterministic()).toBeTrue();
            });

        });

        describe("arc-nondeterministic", () -> {

            before(() -> {
                builder = newBuilder();
                builder.addArc(n0, n1, label);
                builder.addArc(n1, n1, label);
                builder.addArc(n1, n2, label);
                builder.addArc(n1, n3, epsilon);
                builder.addArc(n2, n1, epsilon);
                graph = builder.build();
            });

            describe("#size", () -> {

                it("returns the number of its arcs", () -> {
                    expect(graph.size()).toEqual(5);
                    expect(graph.size()).toEqual(5); // should be cached
                });

            });

            describe("#isEmpty", () -> {

                it("meets a minimum expectation", () -> {
                    expect(newBuilder().build().isEmpty()).toBeTrue();
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
                    expect(graph.referredArcLabels().containsAllArguments(epsilon, label)).toBeTrue();
                });

            });

            describe("#epsilonLabel", () -> {

                it("returns the epsilon label", () -> {
                    expect(graph.epsilonLabel()).toEqual(epsilon);
                });

            });

            describe("#arcDeterministic", () -> {

                it("returns false 1", () -> {
                    expect(graph.arcDeterministic()).toBeFalse();
                    expect(graph.arcDeterministic()).toBeFalse(); // should be cached
                });

                it("returns false 2", () -> {
                    builder = newBuilder();
                    builder.addArc(n1, n1, label);
                    builder.addArc(n1, n2, label);
                    expect(builder.build().arcDeterministic()).toBeFalse();
                });

            });

            describe("#enableArcsOn", () -> {

                it("returns the arcs including epsilon", () -> {
                    final SetIterable<Object> n1Arcs = graph.arcLabelsFrom(n1);
                    final SetIterable<Object> n2Arcs = graph.arcLabelsFrom(n2);
                    expect(n1Arcs.size()).toEqual(2);
                    expect(n1Arcs.containsAllArguments(epsilon, label)).toBeTrue();
                    expect(n2Arcs.size()).toEqual(1);
                    expect(n2Arcs.contains(epsilon)).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(graph.arcLabelsFrom(n3).isEmpty()).toBeTrue();
                });

            });

            describe("#nonEpsilonArcLabelsFrom", () -> {

                it("returns the non-epsilon arcs", () -> {
                    final SetIterable<Object> n1Arcs = graph.nonEpsilonArcLabelsFrom(n1);
                    expect(n1Arcs.size()).toEqual(1);
                    expect(n1Arcs.contains(label)).toBeTrue();
                    expect(graph.nonEpsilonArcLabelsFrom(n2).isEmpty()).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(graph.nonEpsilonArcLabelsFrom(n3).isEmpty()).toBeTrue();
                });

            });

            describe("#arcLabeledFrom", () -> {

                it("returns whether an arc exists", () -> {
                    expect(graph.arcLabeledFrom(n1, epsilon)).toBeTrue();
                    expect(graph.arcLabeledFrom(n1, label)).toBeTrue();
                    expect(graph.arcLabeledFrom(n2, epsilon)).toBeTrue();
                    expect(graph.arcLabeledFrom(n2, label)).toBeFalse();
                    expect(graph.arcLabeledFrom(n3, epsilon)).toBeFalse();
                    expect(graph.arcLabeledFrom(n3, label)).toBeFalse();
                });

            });

            describe("#directSuccessorsOf(node)", () -> {

                it("returns the successors", () -> {
                    final SetIterable<Object> succs1 = graph.directSuccessorsOf(n1);
                    final SetIterable<Object> succs2 = graph.directSuccessorsOf(n2);
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
                    final ImmutableSet<Object> succs1 = graph.directSuccessorsOf(n1, label);
                    final ImmutableSet<Object> succs2 = graph.directSuccessorsOf(n2, epsilon);
                    expect(succs1.size()).toEqual(2);
                    expect(succs1.containsAllArguments(n1, n2)).toBeTrue();
                    expect(succs2.size()).toEqual(1);
                    expect(succs2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(graph.directSuccessorsOf(n3, epsilon).isEmpty()).toBeTrue();
                    expect(graph.directSuccessorsOf(n3, label).isEmpty()).toBeTrue();
                });

            });

            describe("#directSuccessorsOf(nodes, label)", () -> {

                it("returns the successors", () -> {
                    final SetIterable<Object> set1 = Sets.immutable.of(n1, n3);
                    final SetIterable<Object> succs1 = graph.directSuccessorsOf(set1, label);
                    final SetIterable<Object> set2 = Sets.immutable.of(n1, n2);
                    final SetIterable<Object> succs2 = graph.directSuccessorsOf(set2, epsilon);
                    expect(succs1.size()).toEqual(2);
                    expect(succs1.containsAllArguments(n1, n2)).toBeTrue();
                    expect(succs2.size()).toEqual(2);
                    expect(succs2.containsAllArguments(n1, n3)).toBeTrue();
                });

                it("returns empty if it's a dead end singleton", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.of(n3);
                    expect(graph.directSuccessorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(graph.directSuccessorsOf(set, label).isEmpty()).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(graph.directSuccessorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(graph.directSuccessorsOf(set, label).isEmpty()).toBeTrue();
                });

            });

            describe("#directSuccessorOf(node, label)", () -> {

                it("complains", () -> {
                    expect(() -> graph.directSuccessorOf(n1, label)).toThrow(UnsupportedOperationException.class);
                });

            });

            describe("#directPredecessorsOf(node)", () -> {

                it("returns the predecessors", () -> {
                    final SetIterable<Object> preds1 = graph.directPredecessorsOf(n1);
                    final SetIterable<Object> preds2 = graph.directPredecessorsOf(n2);
                    expect(preds1.size()).toEqual(3);
                    expect(preds1.containsAllArguments(n0, n1, n2)).toBeTrue();
                    expect(preds2.size()).toEqual(1);
                    expect(preds2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a start node", () -> {
                    expect(graph.directPredecessorsOf(n0).isEmpty()).toBeTrue();
                });

            });

            describe("#directPredecessorsOf(node, label)", () -> {

                it("returns the predecessors", () -> {
                    final ImmutableSet<Object> n1preds1 = graph.directPredecessorsOf(n1, epsilon);
                    final ImmutableSet<Object> n1preds2 = graph.directPredecessorsOf(n1, label);
                    final ImmutableSet<Object> n2preds1 = graph.directPredecessorsOf(n2, epsilon);
                    final ImmutableSet<Object> n2preds2 = graph.directPredecessorsOf(n2, label);
                    expect(n1preds1.size()).toEqual(1);
                    expect(n1preds1.contains(n2)).toBeTrue();
                    expect(n1preds2.size()).toEqual(2);
                    expect(n1preds2.containsAllArguments(n0, n1)).toBeTrue();
                    expect(n2preds1.isEmpty()).toBeTrue();
                    expect(n2preds2.size()).toEqual(1);
                    expect(n2preds2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a start node", () -> {
                    expect(graph.directPredecessorsOf(n0, epsilon).isEmpty()).toBeTrue();
                    expect(graph.directPredecessorsOf(n0, label).isEmpty()).toBeTrue();
                });

            });

            describe("#directPredecessorsOf(nodes, label)", () -> {

                it("returns the predecessors", () -> {
                    final SetIterable<Object> set1 = Sets.immutable.of(n1, n3);
                    final SetIterable<Object> preds1 = graph.directPredecessorsOf(set1, label);
                    final SetIterable<Object> set2 = Sets.immutable.of(n1, n2);
                    final SetIterable<Object> preds2 = graph.directPredecessorsOf(set2, epsilon);
                    expect(preds1.size()).toEqual(2);
                    expect(preds1.containsAllArguments(n0, n1)).toBeTrue();
                    expect(preds2.size()).toEqual(1);
                    expect(preds2.contains(n2)).toBeTrue();
                });

                it("returns empty if it's a dead end singleton", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.of(n0);
                    expect(graph.directPredecessorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(graph.directPredecessorsOf(set, label).isEmpty()).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(graph.directPredecessorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(graph.directPredecessorsOf(set, label).isEmpty()).toBeTrue();
                });

            });

            describe("#epsilonClosureOf(nodes)", () -> {

                it("returns the closure", () -> {
                    final ImmutableSet<Object> set1 = Sets.immutable.of(n0);
                    final SetIterable<Object> closure1 = graph.epsilonClosureOf(set1);
                    final ImmutableSet<Object> set2 = Sets.immutable.of(n1);
                    final SetIterable<Object> closure2 = graph.epsilonClosureOf(set2);
                    final ImmutableSet<Object> set3 = Sets.immutable.of(n2);
                    final SetIterable<Object> closure3 = graph.epsilonClosureOf(set3);
                    expect(closure1.size()).toEqual(1);
                    expect(closure1.contains(n0)).toBeTrue();
                    expect(closure2.size()).toEqual(2);
                    expect(closure2.containsAllArguments(n1, n3)).toBeTrue();
                    expect(closure3.size()).toEqual(3);
                    expect(closure3.containsAllArguments(n1, n2, n3)).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(graph.epsilonClosureOf(set).isEmpty()).toBeTrue();
                });

            });

            describe("#epsilonClosureOf(nodes, label)", () -> {

                it("returns the closure", () -> {
                    final ImmutableSet<Object> set1 = Sets.immutable.of(n0);
                    final SetIterable<Object> closure1 = graph.epsilonClosureOf(set1, label);
                    final ImmutableSet<Object> set2 = Sets.immutable.of(n1);
                    final SetIterable<Object> closure2 = graph.epsilonClosureOf(set2, epsilon);
                    final ImmutableSet<Object> set3 = Sets.immutable.of(n2);
                    final SetIterable<Object> closure3 = graph.epsilonClosureOf(set3, label);
                    expect(closure1.size()).toEqual(2);
                    expect(closure1.containsAllArguments(n1, n3)).toBeTrue();
                    expect(closure2.size()).toEqual(2);
                    expect(closure2.containsAllArguments(n1, n3)).toBeTrue();
                    expect(closure3.size()).toEqual(3);
                    expect(closure3.containsAllArguments(n1, n2, n3)).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(graph.epsilonClosureOf(set, label).isEmpty()).toBeTrue();
                });

            });

        });

        describe("arc-deterministic", () -> {

            before(() -> {
                builder = newBuilder();
                builder.addArc(n0, n1, label);
                builder.addArc(n1, n2, label);
                graph = builder.build();
            });

            describe("#arcDeterministic", () -> {

                it("returns true", () -> {
                    expect(graph.arcDeterministic()).toBeTrue();
                    expect(graph.arcDeterministic()).toBeTrue(); // should be cached
                });

            });

            describe("#epsilonClosureOf(nodes)", () -> {

                it("complains", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.of(n0);
                    expect(() -> graph.epsilonClosureOf(set)).toThrow(UnsupportedOperationException.class);
                });

            });

            describe("#epsilonClosureOf(nodes, label)", () -> {

                it("complains", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.of(n0);
                    expect(() -> graph.epsilonClosureOf(set, label)).toThrow(UnsupportedOperationException.class);
                });

            });

        });
    }
}
