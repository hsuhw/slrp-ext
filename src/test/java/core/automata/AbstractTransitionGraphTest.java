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
    protected TransitionGraph<Object, Object> instance;

    protected abstract Builder<Object, Object> builderForCommonTest();

    {
        final Object n0 = new Object();
        final Object n1 = new Object();
        final Object n2 = new Object();
        final Object n3 = new Object();
        final Object label = new Object();

        describe("Using the builder", () -> {

            beforeEach(() -> {
                builder = builderForCommonTest();
            });

            describe("#build", () -> {

                it("complains when nothing has been specified", () -> {
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

                it("meets a minimum expectation", () -> {
                    builder.addArc(n0, n0, label);
                    expect(builder.build()).toBeInstanceOf(MapMapSetGraph.class);
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
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

                it("meets a minimum expectation", () -> {
                    builder.addArc(n1, n1, label);
                    builder.addArc(n1, n2, label);
                    expect(builder.build().size()).toEqual(2);
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
                    expect(builder.build().size()).toEqual(1);

                    builder.removeArc(n1, n1, label);
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

            });

            describe("#removeNode", () -> {

                it("only accepts non-null argument", () -> {
                    expect(() -> builder.removeNode(nullArg)).toThrow(IllegalArgumentException.class);
                });

                it("complains when removing a non-existing node", () -> {
                    expect(() -> builder.removeNode(n1)).toThrow(Exception.class);

                    builder.addArc(n1, n1, label);
                    expect(() -> builder.removeNode(n2)).toThrow(Exception.class);
                });

                it("removes forward-only nodes", () -> {
                    builder.addArc(n1, n2, label);
                    builder.removeNode(n1);
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

                it("removes backward-only nodes", () -> {
                    builder.addArc(n1, n2, label);
                    builder.removeNode(n2);
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

                it("removes for/backward-removing nodes", () -> {
                    builder.addArc(n1, n2, label);
                    builder.addArc(n2, n1, label);
                    builder.removeNode(n1);
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

            });

        });

        describe("arc-nondeterministic", () -> {

            before(() -> {
                builder = builderForCommonTest();
                builder.addArc(n0, n1, label);
                builder.addArc(n1, n1, label);
                builder.addArc(n1, n2, label);
                builder.addArc(n1, n3, epsilon);
                builder.addArc(n2, n1, epsilon);
                instance = builder.build();
            });

            describe("#size", () -> {

                it("returns the number of its arcs", () -> {
                    expect(instance.size()).toEqual(5);
                    expect(instance.size()).toEqual(5); // should be cached, hard to tell though
                });

            });

            describe("#referredNodes", () -> {

                it("returns the nodes", () -> {
                    expect(instance.referredNodes().size()).toEqual(4);  // should be cached
                    expect(instance.referredNodes().containsAllArguments(n0, n1, n2, n3)).toBeTrue();
                });

            });

            describe("#referredArcLabels", () -> {

                it("returns the labels", () -> {
                    expect(instance.referredArcLabels().size()).toEqual(2); // should be cached
                    expect(instance.referredArcLabels().containsAllArguments(epsilon, label)).toBeTrue();
                });

            });

            describe("#epsilonLabel", () -> {

                it("returns the epsilon label", () -> {
                    expect(instance.epsilonLabel()).toEqual(epsilon);
                });

            });

            describe("#arcDeterministic", () -> {

                it("returns false 1", () -> {
                    expect(instance.arcDeterministic()).toBeFalse();
                    expect(instance.arcDeterministic()).toBeFalse(); // should be cached, hard to tell though
                });

                it("returns false 2", () -> {
                    builder = builderForCommonTest();
                    builder.addArc(n1, n1, label);
                    builder.addArc(n1, n2, label);
                    expect(builder.build().arcDeterministic()).toBeFalse();
                });

            });

            describe("#enableArcsOn", () -> {

                it("returns the arcs including epsilon", () -> {
                    final SetIterable<Object> n1Arcs = instance.enabledArcsOn(n1);
                    final SetIterable<Object> n2Arcs = instance.enabledArcsOn(n2);
                    expect(n1Arcs.size()).toEqual(2);
                    expect(n1Arcs.containsAllArguments(epsilon, label)).toBeTrue();
                    expect(n2Arcs.size()).toEqual(1);
                    expect(n2Arcs.contains(epsilon)).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(instance.enabledArcsOn(n3).isEmpty()).toBeTrue();
                });

            });

            describe("#nonEpsilonArcsOn", () -> {

                it("returns the non-epsilon arcs", () -> {
                    final SetIterable<Object> n1Arcs = instance.nonEpsilonArcsOn(n1);
                    expect(n1Arcs.size()).toEqual(1);
                    expect(n1Arcs.contains(label)).toBeTrue();
                    expect(instance.nonEpsilonArcsOn(n2).isEmpty()).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(instance.nonEpsilonArcsOn(n3).isEmpty()).toBeTrue();
                });

            });

            describe("#hasArc", () -> {

                it("returns whether an arc exists", () -> {
                    expect(instance.hasArc(n1, epsilon)).toBeTrue();
                    expect(instance.hasArc(n1, label)).toBeTrue();
                    expect(instance.hasArc(n2, epsilon)).toBeTrue();
                    expect(instance.hasArc(n2, label)).toBeFalse();
                    expect(instance.hasArc(n3, epsilon)).toBeFalse();
                    expect(instance.hasArc(n3, label)).toBeFalse();
                });

            });

            describe("#successorsOf(node)", () -> {

                it("returns the successors", () -> {
                    final SetIterable<Object> succs1 = instance.successorsOf(n1);
                    final SetIterable<Object> succs2 = instance.successorsOf(n2);
                    expect(succs1.size()).toEqual(3);
                    expect(succs1.containsAllArguments(n1, n2, n3)).toBeTrue();
                    expect(succs2.size()).toEqual(1);
                    expect(succs2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(instance.successorsOf(n3).isEmpty()).toBeTrue();
                });

            });

            describe("#successorsOf(node, label)", () -> {

                it("returns the successors", () -> {
                    final ImmutableSet<Object> succs1 = instance.successorsOf(n1, label);
                    final ImmutableSet<Object> succs2 = instance.successorsOf(n2, epsilon);
                    expect(succs1.size()).toEqual(2);
                    expect(succs1.containsAllArguments(n1, n2)).toBeTrue();
                    expect(succs2.size()).toEqual(1);
                    expect(succs2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a dead end", () -> {
                    expect(instance.successorsOf(n3, epsilon).isEmpty()).toBeTrue();
                    expect(instance.successorsOf(n3, label).isEmpty()).toBeTrue();
                });

            });

            describe("#successorsOf(nodes, label)", () -> {

                it("returns the successors", () -> {
                    final SetIterable<Object> set1 = Sets.immutable.of(n1, n3);
                    final SetIterable<Object> succs1 = instance.successorsOf(set1, label);
                    final SetIterable<Object> set2 = Sets.immutable.of(n1, n2);
                    final SetIterable<Object> succs2 = instance.successorsOf(set2, epsilon);
                    expect(succs1.size()).toEqual(2);
                    expect(succs1.containsAllArguments(n1, n2)).toBeTrue();
                    expect(succs2.size()).toEqual(2);
                    expect(succs2.containsAllArguments(n1, n3)).toBeTrue();
                });

                it("returns empty if it's a dead end singleton", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.of(n3);
                    expect(instance.successorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(instance.successorsOf(set, label).isEmpty()).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(instance.successorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(instance.successorsOf(set, label).isEmpty()).toBeTrue();
                });

            });

            describe("#successorOf(node, label)", () -> {

                it("complains", () -> {
                    expect(() -> instance.successorOf(n1, label)).toThrow(UnsupportedOperationException.class);
                });

            });

            describe("#predecessorsOf(node)", () -> {

                it("returns the predecessors", () -> {
                    final SetIterable<Object> preds1 = instance.predecessorsOf(n1);
                    final SetIterable<Object> preds2 = instance.predecessorsOf(n2);
                    expect(preds1.size()).toEqual(3);
                    expect(preds1.containsAllArguments(n0, n1, n2)).toBeTrue();
                    expect(preds2.size()).toEqual(1);
                    expect(preds2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a start node", () -> {
                    expect(instance.predecessorsOf(n0).isEmpty()).toBeTrue();
                });

            });

            describe("#predecessorsOf(node, label)", () -> {

                it("returns the predecessors", () -> {
                    final ImmutableSet<Object> n1preds1 = instance.predecessorsOf(n1, epsilon);
                    final ImmutableSet<Object> n1preds2 = instance.predecessorsOf(n1, label);
                    final ImmutableSet<Object> n2preds1 = instance.predecessorsOf(n2, epsilon);
                    final ImmutableSet<Object> n2preds2 = instance.predecessorsOf(n2, label);
                    expect(n1preds1.size()).toEqual(1);
                    expect(n1preds1.contains(n2)).toBeTrue();
                    expect(n1preds2.size()).toEqual(2);
                    expect(n1preds2.containsAllArguments(n0, n1)).toBeTrue();
                    expect(n2preds1.isEmpty()).toBeTrue();
                    expect(n2preds2.size()).toEqual(1);
                    expect(n2preds2.contains(n1)).toBeTrue();
                });

                it("returns empty if it's a start node", () -> {
                    expect(instance.predecessorsOf(n0, epsilon).isEmpty()).toBeTrue();
                    expect(instance.predecessorsOf(n0, label).isEmpty()).toBeTrue();
                });

            });

            describe("#predecessorsOf(nodes, label)", () -> {

                it("returns the predecessors", () -> {
                    final SetIterable<Object> set1 = Sets.immutable.of(n1, n3);
                    final SetIterable<Object> preds1 = instance.predecessorsOf(set1, label);
                    final SetIterable<Object> set2 = Sets.immutable.of(n1, n2);
                    final SetIterable<Object> preds2 = instance.predecessorsOf(set2, epsilon);
                    expect(preds1.size()).toEqual(2);
                    expect(preds1.containsAllArguments(n0, n1)).toBeTrue();
                    expect(preds2.size()).toEqual(1);
                    expect(preds2.contains(n2)).toBeTrue();
                });

                it("returns empty if it's a dead end singleton", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.of(n0);
                    expect(instance.predecessorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(instance.predecessorsOf(set, label).isEmpty()).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(instance.predecessorsOf(set, epsilon).isEmpty()).toBeTrue();
                    expect(instance.predecessorsOf(set, label).isEmpty()).toBeTrue();
                });

            });

            describe("#epsilonClosureOf(nodes)", () -> {

                it("returns the closure", () -> {
                    final ImmutableSet<Object> set1 = Sets.immutable.of(n0);
                    final SetIterable<Object> closure1 = instance.epsilonClosureOf(set1);
                    final ImmutableSet<Object> set2 = Sets.immutable.of(n1);
                    final SetIterable<Object> closure2 = instance.epsilonClosureOf(set2);
                    final ImmutableSet<Object> set3 = Sets.immutable.of(n2);
                    final SetIterable<Object> closure3 = instance.epsilonClosureOf(set3);
                    expect(closure1.size()).toEqual(1);
                    expect(closure1.contains(n0)).toBeTrue();
                    expect(closure2.size()).toEqual(2);
                    expect(closure2.containsAllArguments(n1, n3)).toBeTrue();
                    expect(closure3.size()).toEqual(3);
                    expect(closure3.containsAllArguments(n1, n2, n3)).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(instance.epsilonClosureOf(set).isEmpty()).toBeTrue();
                });

            });

            describe("#epsilonClosureOf(nodes, label)", () -> {

                it("returns the closure", () -> {
                    final ImmutableSet<Object> set1 = Sets.immutable.of(n0);
                    final SetIterable<Object> closure1 = instance.epsilonClosureOf(set1, label);
                    final ImmutableSet<Object> set2 = Sets.immutable.of(n1);
                    final SetIterable<Object> closure2 = instance.epsilonClosureOf(set2, epsilon);
                    final ImmutableSet<Object> set3 = Sets.immutable.of(n2);
                    final SetIterable<Object> closure3 = instance.epsilonClosureOf(set3, label);
                    expect(closure1.size()).toEqual(2);
                    expect(closure1.containsAllArguments(n1, n3)).toBeTrue();
                    expect(closure2.size()).toEqual(2);
                    expect(closure2.containsAllArguments(n1, n3)).toBeTrue();
                    expect(closure3.size()).toEqual(3);
                    expect(closure3.containsAllArguments(n1, n2, n3)).toBeTrue();
                });

                it("returns empty if an empty is given", () -> {
                    final ImmutableSet<Object> set = Sets.immutable.empty();
                    expect(instance.epsilonClosureOf(set, label).isEmpty()).toBeTrue();
                });

            });

        });

        describe("arc-deterministic", () -> {

            before(() -> {
                builder = builderForCommonTest();
                builder.addArc(n0, n1, label);
                builder.addArc(n1, n2, label);
                instance = builder.build();
            });

            describe("#arcDeterministic", () -> {

                it("returns true", () -> {
                    expect(instance.arcDeterministic()).toBeTrue();
                    expect(instance.arcDeterministic()).toBeTrue(); // should be cached, hard to tell though
                });

            });

        });
    }
}
