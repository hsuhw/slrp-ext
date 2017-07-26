package synth;

import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.list.primitive.IntInterval;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractSatSolverTest
{
    SatSolver solver;

    private void expectSolutionExists()
    {
        expect(solver.findModel().isPresent()).toBeTrue();
    }

    private void expectNoSolutionExists()
    {
        expect(solver.findModel().isPresent()).toBeFalse();
    }

    private ImmutableIntSet model()
    {
        return solver.getModel();
    }

    {
        afterEach(() -> solver.reset());

        describe("#newFreeVariables()", () -> {

            it("should provide an integer interval of the new variables in question", () -> {
                final IntInterval interval = solver.newFreeVariables(3);
                expect(interval).toBeNotNull();
                expect(interval.containsAll(1, 2, 3)).toBeTrue();
                expect(interval.containsNone(-1, 0, 4, 5)).toBeTrue();
            });

            it("should provide an singleton interval when called with one", () -> {
                final IntInterval singletonInterval = solver.newFreeVariables(1);
                expect(singletonInterval).toBeNotNull();
                expect(singletonInterval.size()).toEqual(1);
                expect(singletonInterval.contains(1)).toBeTrue();
                expect(singletonInterval.containsNone(-1, 0, 2, 3)).toBeTrue();
            });

            it("should throw an exception when called with zero or a negative integer number", () -> {
                expect(() -> solver.newFreeVariables(0)).toThrow(IllegalArgumentException.class);
                expect(() -> solver.newFreeVariables(-1)).toThrow(IllegalArgumentException.class);
            });

            it("should throw an exception when the solver instance runs out of free variables", () -> {
                expect(() -> solver.newFreeVariables(SatSolver.MAX_CLAUSE_NUMBER + 1))
                    .toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#addClause(int...)", () -> {

            it("should add a DIMACS-CNF format clause correctly", () -> {
                solver.addClause(1, 2);
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1, 3);
                solver.addClause(-3);
                expectNoSolutionExists();
            });

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(1);
                expect(() -> solver.addClause(-1)).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#addClause(IntInterval)", () -> {

            it("should work the same as #addClause(int...)", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1, 3);
                solver.addClause(-3);
                expectNoSolutionExists();
            });

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(-1);
                expect(() -> solver.addClause(solver.newFreeVariables(1))).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#setLiteralTruthy(int)", () -> {

            it("should ensure the given literal be assigned true in the model", () -> {
                solver.addClause(1, 2);
                solver.setLiteralTruthy(-2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1, 3);
                solver.setLiteralTruthy(-3);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                expect(() -> solver.setLiteralTruthy(-1)).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#setLiteralsTruthy(int...)", () -> {

            it("should ensure the given literals be assigned true in the model", () -> {
                solver.addClause(1, 2, 3);
                solver.setLiteralsTruthy(-2, -3);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1, 4, 5);
                solver.setLiteralsTruthy(-4, -5);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.setLiteralsTruthy(1, 2);
                expect(() -> solver.setLiteralsTruthy(-1, -2)).toThrow(UnsupportedOperationException.class);
            });

        });
        describe("#setLiteralFalsy(int)", () -> {

            it("should ensure the given literal be assigned true in the model", () -> {
                solver.addClause(1, 2);
                solver.setLiteralFalsy(2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1, 3);
                solver.setLiteralFalsy(3);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                expect(() -> solver.setLiteralFalsy(1)).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#setLiteralsFalsy(int...)", () -> {

            it("should ensure the given literals be assigned true in the model", () -> {
                solver.addClause(1, 2, 3);
                solver.setLiteralsFalsy(2, 3);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1, 4, 5);
                solver.setLiteralsFalsy(4, 5);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.setLiteralsFalsy(1, 2);
                expect(() -> solver.setLiteralsFalsy(-1, -2)).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#markAsEquivalent(int, int)", () -> {

            it("should ensure the literals always be assigned the same value in the model", () -> {
                // same as true
                solver.addClause(-1, -2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(1);
                expectNoSolutionExists();

                solver.reset();

                // same as false
                solver.addClause(1, 2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                expect(() -> solver.markAsEquivalent(1, 2)).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#markEachAsEquivalent(int...)", () -> {

            it("should ensure the literals always be assigned the same value in the model", () -> {
                // same as true
                solver.addClause(-1, -2, -3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(1);
                expectNoSolutionExists();

                solver.reset();

                // same as false
                solver.addClause(1, 2, 3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                solver.addClause(3);
                expect(() -> solver.markEachAsEquivalent(1, 2, 3)).toThrow(UnsupportedOperationException.class);
            });

        });

        describe("#addClauseAtLeast(IntInterval)", () -> {

            it("should work the same as #addClauseAtLeast(int...)", () -> {
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
                solver.addClause(-4);
                expectNoSolutionExists();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                expectNoSolutionExists();
            });

        });

        describe("#addClauseAtLeastIf(int, int, IntInterval)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseAtLeast(IntInterval)", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtLeastIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(-2);
                    solver.addClause(-3);
                    expectSolutionExists();
                    expect(model().containsAll(4, 5)).toBeTrue();

                    solver.addClause(-4);
                    solver.addClause(-5);
                    expectNoSolutionExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtLeastIf(yes, 2, solver.newFreeVariables(4));
                    expectNoSolutionExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtLeastIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    solver.addClause(-4);
                    solver.addClause(-5);
                    solver.addClause(-6);
                    solver.addClause(-7);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtLeastIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    expectNoSolutionExists();
                });

            });

        });


        describe("#addClauseAtMost(IntInterval)", () -> {

            it("should work the same as #addClauseAtMost(int...)", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectSolutionExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoSolutionExists();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                expectNoSolutionExists();
            });

        });

        describe("#addClauseAtMostIf(int, int, IntInterval)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseAtMost(IntInterval)", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtMostIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(2);
                    solver.addClause(3);
                    expectSolutionExists();
                    expect(model().containsAll(-4, -5)).toBeTrue();

                    solver.addClause(4);
                    expectNoSolutionExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtMostIf(yes, 2, solver.newFreeVariables(4));
                    expectNoSolutionExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtMostIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    solver.addClause(4);
                    solver.addClause(5);
                    solver.addClause(6);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtMostIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    expectNoSolutionExists();
                });

            });

        });

        describe("#addClauseExactly(int, IntInterval)", () -> {

            it("should work the same as #addClauseExactly(int, int...)", () -> {
                // no more
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectSolutionExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoSolutionExists();

                solver.reset();

                // nor less
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
                expectNoSolutionExists();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoSolutionExists();

                solver.addClauseExactly(2, solver.newFreeVariables(4));
                expectNoSolutionExists();
            });

        });

        describe("#addClauseExactlyIf(int, int, IntInterval)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseExactly(int, int, int...)", () -> {
                    // no more
                    int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseExactlyIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(2);
                    solver.addClause(3);
                    expectSolutionExists();
                    expect(model().containsAll(-4, -5)).toBeTrue();

                    solver.addClause(4);
                    expectNoSolutionExists();

                    solver.reset();

                    // nor less
                    yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseExactlyIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(-2);
                    solver.addClause(-3);
                    expectSolutionExists();
                    expect(model().containsAll(4, 5)).toBeTrue();

                    solver.addClause(-4);
                    expectNoSolutionExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseExactlyIf(yes, 2, solver.newFreeVariables(4));
                    expectNoSolutionExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseExactlyIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    solver.addClause(4);
                    solver.addClause(5);
                    solver.addClause(6);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseExactlyIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    expectNoSolutionExists();
                });

            });

        });

        describe("#addClauseBlocking(int...)", () -> {

            it("should prevent the given clause showing up as an model", () -> {
                solver.addClause(1, 2);
                solver.addClauseBlocking(1, 2);
                solver.addClauseBlocking(-1, 2);
                expectSolutionExists();
                expect(model().containsAll(1, -2)).toBeTrue();

                solver.addClause(2);
                expectNoSolutionExists();
            });

            it("can loop to keep blocking a problem until it is made unsatisfiable", () -> {
                solver.addClause(1, 2, 3, 4);
                while (solver.findModel().isPresent()) {
                    solver.addClauseBlocking(model().toArray());
                }
                expectNoSolutionExists();
            });

        });

        describe("#addClauseBlockingIf(int, int...)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseBlocking(int...)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3);
                    solver.addClauseBlockingIf(1, 2, 3);
                    solver.addClauseBlockingIf(1, -2, 3);
                    expectSolutionExists();
                    expect(model().containsAll(2, -3)).toBeTrue();

                    solver.addClause(3);
                    expectNoSolutionExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    solver.addClause(3);
                    solver.addClauseBlockingIf(3, 4, 5);
                    expectNoSolutionExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-3);
                    solver.addClauseBlockingIf(3, 4, 5);
                    solver.addClause(4);
                    solver.addClause(5);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    solver.addClause(-3);
                    solver.addClauseBlockingIf(3, 4, 5);
                    expectNoSolutionExists();
                });

            });

        });

        describe("#addImplications(int, int...)", () -> {

            it("should add the implication as expected", () -> {
                // antecedent is true
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(1);
                expectSolutionExists();
                expect(model().containsAll(2, 3)).toBeTrue();

                solver.addClause(-2);
                expectNoSolutionExists();

                solver.reset();

                // antecedent is false
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(3);
                expectSolutionExists();
            });

            it("should not affect the established satisfiability", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectSolutionExists();
                expect(model().contains(2)).toBeTrue();

                solver.addImplications(3, 4, 5);
                expectSolutionExists();
                expect(model().contains(2)).toBeTrue();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoSolutionExists();

                solver.addImplications(3, 4, 5);
                expectNoSolutionExists();
            });

        });

        describe("#addImplicationsIf(int, int, int...)", () -> {

            describe("when activated", () -> {

                it("should add the implication as expected", () -> {
                    // antecedent is true
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(2);
                    expectSolutionExists();
                    expect(model().containsAll(3, 4)).toBeTrue();

                    solver.addClause(-3);
                    expectNoSolutionExists();

                    solver.reset();

                    // antecedent is false
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(-2);
                    solver.addClause(-3);
                    solver.addClause(4);
                    expectSolutionExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    solver.addClause(3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    expectNoSolutionExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    solver.addClause(4);
                    solver.addClause(-5);
                    solver.addClause(6);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoSolutionExists();

                    solver.addClause(-3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    expectNoSolutionExists();
                });

            });

        });
    }
}
