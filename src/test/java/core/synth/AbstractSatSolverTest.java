package core.synth;

import api.synth.SatSolver;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;

public abstract class AbstractSatSolverTest
{
    protected SatSolver solver;

    protected void expectSolutionExists()
    {
        expect(solver.findModel()).toBeNotNull();
    }

    protected void expectNoSolutionExists()
    {
        expect(solver.findModel()).toBeNull();
    }

    protected ImmutableIntSet model()
    {
        return solver.getModel();
    }

    {
        afterEach(() -> solver.reset());

        describe("#newFreeVariables()", () -> {

            it("should provide an integer interval of the new variables in question", () -> {
                final ImmutableIntList interval = solver.newFreeVariables(3);
                expect(interval).toBeNotNull();
                expect(interval.size()).toEqual(3);
                expect(interval.containsAll(1, 2, 3)).toBeTrue();
            });

            it("should provide an singleton interval when called with one", () -> {
                final ImmutableIntList singletonInterval = solver.newFreeVariables(1);
                expect(singletonInterval).toBeNotNull();
                expect(singletonInterval.size()).toEqual(1);
                expect(singletonInterval.contains(1)).toBeTrue();
            });

            it("should throw an exception when called with zero or a negative integer number", () -> {
                expect(() -> solver.newFreeVariables(0)).toThrow(IllegalArgumentException.class);
                expect(() -> solver.newFreeVariables(-1)).toThrow(IllegalArgumentException.class);
            });

            it("should throw an exception when the solver instance runs out of free variables", () -> {
                expect(() -> solver.newFreeVariables(SatSolver.MAX_CLAUSE_NUMBER + 1))
                    .toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClause(int...)", () -> {

            it("should add a DIMACS-CNF format clause correctly", () -> {
                solver.addClause(1, 2);
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(1);
                expect(() -> solver.addClause(-1)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClause(ImmutableIntList)", () -> {

            it("should work the same as #addClause(int...)", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(-1);
                expect(() -> solver.addClause(solver.newFreeVariables(1))).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClauseIf(int, ImmutableIntList)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClause(ImmutableIntList)", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseIf(yes, solver.newFreeVariables(2));
                    solver.addClause(-3);
                    expectSolutionExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-2);
                    expectNoSolutionExists();
                });

                it("should throw an exception when the clause causes an immediate contradiction", () -> {
                    expect(() -> {
                        final int yes = solver.newFreeVariables(1).getFirst();
                        solver.addClause(yes);
                        solver.addClause(-2);
                        solver.addClauseIf(yes, solver.newFreeVariables(1));
                    }).toThrow(IllegalArgumentException.class);
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
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(2));
                    solver.addClause(-4);
                    solver.addClause(-5);
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
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(2));
                    expectNoSolutionExists();
                });

                it("should not throw an exception when the clause causes an immediate contradiction", () -> {
                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClause(-2);
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(1));
                });

            });

        });

        describe("#setLiteralTruthy(int)", () -> {

            it("should ensure the given literal be assigned true in the model", () -> {
                solver.addClause(1, 2);
                solver.setLiteralTruthy(-2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                expect(() -> solver.setLiteralTruthy(-1)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#setLiteralsTruthy(int...)", () -> {

            it("should ensure the given literals be assigned true in the model", () -> {
                solver.addClause(1, 2, 3);
                solver.setLiteralsTruthy(-2, -3);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.setLiteralsTruthy(1, 2);
                expect(() -> solver.setLiteralsTruthy(-1, -2)).toThrow(IllegalArgumentException.class);
            });

        });
        describe("#setLiteralFalsy(int)", () -> {

            it("should ensure the given literal be assigned true in the model", () -> {
                solver.addClause(1, 2);
                solver.setLiteralFalsy(2);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                expect(() -> solver.setLiteralFalsy(1)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#setLiteralsFalsy(int...)", () -> {

            it("should ensure the given literals be assigned true in the model", () -> {
                solver.addClause(1, 2, 3);
                solver.setLiteralsFalsy(2, 3);
                expectSolutionExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.setLiteralsFalsy(1, 2);
                expect(() -> solver.setLiteralsFalsy(-1, -2)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#markAsEquivalent(int, int)", () -> {

            it("should ensure the literals be assigned the same value (the true case)", () -> {
                solver.addClause(-1, -2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(1);
                expectNoSolutionExists();
            });

            it("should ensure the literals be assigned the same value (the false case)", () -> {
                solver.addClause(1, 2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                expect(() -> solver.markAsEquivalent(1, 2)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#markEachAsEquivalent(int...)", () -> {

            it("should ensure the literals be assigned the same value (the true case)", () -> {
                // same as true
                solver.addClause(-1, -2, -3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(1);
                expectNoSolutionExists();
            });

            it("should ensure the literals be assigned the same value (the false case)", () -> {
                solver.addClause(1, 2, 3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(-1);
                expectNoSolutionExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                solver.addClause(3);
                expect(() -> solver.markEachAsEquivalent(1, 2, 3)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClauseAtLeast(int, ImmutableIntList)", () -> {

            it("should work the same as #addClauseAtLeast(int, int...)", () -> {
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectSolutionExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
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

        describe("#addClauseAtLeastIf(int, int, ImmutableIntList)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseAtLeast(int, ImmutableIntList)", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtLeastIf(yes, 2, solver.newFreeVariables(4));
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

        describe("#addClauseAtMost(ImmutableIntList)", () -> {

            it("should work the same as #addClauseAtMost(int...)", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectSolutionExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoSolutionExists();
            });

            it("should allow zero true assignments in the given clause", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(-3);
                solver.addClause(-4);
                expectSolutionExists();
                expect(model().containsAll(-1, -2, -3, -4)).toBeTrue();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                expectNoSolutionExists();
            });

        });

        describe("#addClauseAtMostIf(int, int, ImmutableIntList)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseAtMost(int, ImmutableIntList)", () -> {
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

                it("should allow zero true assignments in the given clause", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtMostIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(-2);
                    solver.addClause(-3);
                    solver.addClause(-4);
                    solver.addClause(-5);
                    expectSolutionExists();
                    expect(model().containsAll(-2, -3, -4, -5)).toBeTrue();
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

        describe("#addClauseExactly(int, ImmutableIntList)", () -> {

            it("should ensure no more than the given degree", () -> {
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectSolutionExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoSolutionExists();
            });

            it("should ensure no less than the given degree", () -> {
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

        describe("#addClauseExactlyIf(int, int, ImmutableIntList)", () -> {

            describe("when activated", () -> {

                it("should ensure no more than the given degree", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseExactlyIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(2);
                    solver.addClause(3);
                    expectSolutionExists();
                    expect(model().containsAll(-4, -5)).toBeTrue();

                    solver.addClause(4);
                    expectNoSolutionExists();
                });

                it("should ensure no less than the given degree", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
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
                while (solver.findModel() != null) {
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

            it("should add the implication as expected (true antecedent)", () -> {
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(1);
                expectSolutionExists();
                expect(model().containsAll(2, 3)).toBeTrue();

                solver.addClause(-2);
                expectNoSolutionExists();
            });

            it("should add the implication as expected (false antecedent)", () -> {
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

                it("should add the implication as expected (true antecedent)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(2);
                    expectSolutionExists();
                    expect(model().containsAll(3, 4)).toBeTrue();

                    solver.addClause(-3);
                    expectNoSolutionExists();
                });

                it("should add the implication as expected (false antecedent)", () -> {
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
