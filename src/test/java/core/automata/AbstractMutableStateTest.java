package core.automata;

import api.automata.MutableState;
import api.automata.State;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.SetIterable;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

abstract class AbstractMutableStateTest
{
    private MutableState<Object> state;
    private MutableState<Object> state2;
    private MutableState<Object> state3;
    private final Object a1 = new Object();
    private final Object a2 = new Object();
    private final Object a3 = new Object();
    private final Object nullSymbol = null;
    private final String nullString = null;
    private final MutableState<Object> nullState = null;

    abstract MutableState<Object> newState();

    {
        beforeEach(() -> {
            state = newState();
            state2 = newState();
            state3 = newState();
        });

        describe("#set/#name", () -> {

            it("meets a minimum expectation", () -> {
                final var name1 = "1qaz";
                state.setName(name1);
                expect(state.name()).toEqual(name1);

                final var name2 = "2wsx";
                state.setName(name2);
                expect(state.name()).toEqual(name2);

                expect(() -> state.setName(nullString)).toThrow(NullPointerException.class);
            });

        });

        describe("Transitions", () -> {

            it("can show all transition labels", () -> {
                state.addTransition(a1, state);
                state.addTransition(a1, state2);
                state.addTransition(a2, state2);
                final RichIterable<Object> labels = state.enabledSymbols();
                expect(labels.size()).toEqual(2);
                expect(labels.contains(a1)).toBeTrue();
                expect(labels.contains(a2)).toBeTrue();
                expect(labels.contains(a3)).toBeFalse();
            });

            it("can show certain transition labels", () -> {
                state.addTransition(a1, state);
                state.addTransition(a1, state2);
                state.addTransition(a1, state2);
                state.addTransition(a2, state2);
                final RichIterable<Object> labels1 = state.enabledSymbolsTo(state);
                expect(labels1.size()).toEqual(1);
                expect(labels1.contains(a1)).toBeTrue();
                final RichIterable<Object> labels2 = state.enabledSymbolsTo(state2);
                expect(labels2.size()).toEqual(2);
                expect(labels2.contains(a1)).toBeTrue();
                expect(labels2.contains(a2)).toBeTrue();
                final RichIterable<Object> labels3 = state.enabledSymbolsTo(state3);
                expect(labels3.isEmpty()).toBeTrue();
            });

            it("will not show labels with null given", () -> {
                expect(state.enabledSymbolsTo(nullState).isEmpty()).toBeTrue();
            });

            it("can show transition existences", () -> {
                state.addTransition(a1, state);
                state.addTransition(a2, state);
                state.addTransition(a2, state2);
                expect(state.transitionExists(a1)).toBeTrue();
                expect(state.transitionExists(a2)).toBeTrue();
                expect(state.transitionExists(a3)).toBeFalse();
                expect(state.transitionExists(state)).toBeTrue();
                expect(state.transitionExists(state2)).toBeTrue();
                expect(state.transitionExists(state3)).toBeFalse();
            });

            it("will not show existences with null given", () -> {
                expect(state.transitionExists(nullSymbol)).toBeFalse();
                expect(state.transitionExists(nullState)).toBeFalse();
            });

            it("will not add with null given", () -> {
                expect(() -> state.addTransition(nullSymbol, state)).toThrow(NullPointerException.class);
                expect(() -> state.addTransition(nullSymbol, nullState)).toThrow(NullPointerException.class);
                expect(() -> state.addTransition(a1, nullState)).toThrow(NullPointerException.class);
            });

            it("can remove certain transitions", () -> {
                state.addTransition(a1, state);
                state.addTransition(a2, state);
                state.addTransition(a1, state2);
                state.addTransition(a2, state3);
                state.removeTransitionsTo(state);
                expect(state.enabledSymbols().size()).toEqual(2);
                expect(state.transitionExists(state)).toBeFalse();
                expect(state.transitionExists(state2)).toBeTrue();
                expect(state.transitionExists(state3)).toBeTrue();
            });

            it("will not remove with null given", () -> {
                expect(() -> state.removeTransitionsTo(nullState)).toThrow(NullPointerException.class);
            });

        });

        describe("Successors", () -> {

            it("can show all successors", () -> {
                state.addTransition(a1, state);
                state.addTransition(a2, state);
                state.addTransition(a2, state2);
                final SetIterable<? extends State<Object>> successors = state.successors();
                expect(successors.size()).toEqual(2);
                expect(successors.contains(state)).toBeTrue();
                expect(successors.contains(state2)).toBeTrue();
                expect(successors.contains(state3)).toBeFalse();
            });

            it("can show certain successors", () -> {
                state.addTransition(a1, state);
                state.addTransition(a2, state);
                state.addTransition(a2, state2);
                final SetIterable<? extends State<Object>> successors1 = state.successors(a1);
                expect(successors1.size()).toEqual(1);
                expect(successors1.contains(state)).toBeTrue();
                final SetIterable<? extends State<Object>> successors2 = state.successors(a2);
                expect(successors2.size()).toEqual(2);
                expect(successors2.containsAllArguments(state, state2)).toBeTrue();
                final SetIterable<? extends State<Object>> successors3 = state.successors(a3);
                expect(successors3.isEmpty()).toBeTrue();
            });

            it("will not show successors with null given", () -> {
                expect(state.successors(nullSymbol).isEmpty()).toBeTrue();
            });

        });

        describe("#toMutable", () -> {

            it("returns itself", () -> {
                expect(state.toMutable()).toEqual(state);
            });

        });

        describe("#toImmutable", () -> {

            it("is not implemented yet", () -> {
                expect(() -> state.toImmutable()).toThrow(UnsupportedOperationException.class);
            });

        });
    }
}
