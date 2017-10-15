package core.automata;

import static api.automata.TransitionGraph.Builder;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractTransitionGraphTest
{
    protected final Object nullArg = null;
    protected final Object epsilon = new Object();

    protected Builder<Object, Object> builder;

    protected abstract Builder<Object, Object> builderForCommonTest();

    {
        describe("When using its builder", () -> {

            beforeEach(() -> {
                builder = builderForCommonTest();
            });

            describe("#build", () -> {

                it("complains when nothing has been specified", () -> {
                    expect(builder::build).toThrow(IllegalStateException.class);
                });

                it("meets a minimum expectation", () -> {
                    final Object n = new Object();
                    final Object label = new Object();
                    builder.addArc(n, n, label);

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
                    final Object n = new Object();
                    builder.addArc(n, n, epsilon);

                    expect(builder::build).toThrow(IllegalStateException.class);
                });

                it("meets a minimum expectation", () -> {
                    final Object n1 = new Object();
                    final Object n2 = new Object();
                    final Object label = new Object();
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
                    final Object n1 = new Object();
                    final Object n2 = new Object();
                    final Object label = new Object();

                    expect(() -> builder.removeArc(n1, n2, label)).toThrow(Exception.class);

                    builder.addArc(n1, n1, label);

                    expect(() -> builder.removeArc(n1, n2, label)).toThrow(Exception.class);
                });

                it("meets a minimum expectation", () -> {
                    final Object n1 = new Object();
                    final Object n2 = new Object();
                    final Object label = new Object();
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

                it("complains when removing non-existing node", () -> {
                    final Object n1 = new Object();
                    final Object n2 = new Object();
                    final Object label = new Object();

                    expect(() -> builder.removeNode(n1)).toThrow(Exception.class);

                    builder.addArc(n1, n1, label);

                    expect(() -> builder.removeNode(n2)).toThrow(Exception.class);
                });

                it("meets a minimum expectation", () -> {
                    final Object n1 = new Object();
                    final Object n2 = new Object();
                    final Object label = new Object();
                    builder.addArc(n1, n1, label);
                    builder.addArc(n1, n2, label);
                    builder.removeNode(n1);

                    expect(builder::build).toThrow(IllegalStateException.class);
                });
            });

        });
    }
}
