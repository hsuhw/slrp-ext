package core.synth;

import api.synth.SatSolver;
import api.synth.SatSolverTimeoutException;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static core.util.Parameters.SAT_SOLVER_MAX_CLAUSE_NUMBER;

public abstract class AbstractSatSolverTest
{
    protected SatSolver solver;

    protected boolean modelExists() throws SatSolverTimeoutException
    {
        return solver.findItSatisfiable();
    }

    protected void expectModelExists() throws SatSolverTimeoutException
    {
        expect(solver.findItSatisfiable()).toBeTrue();
    }

    protected void expectNoModelExists() throws SatSolverTimeoutException
    {
        expect(solver.findItSatisfiable()).toBeFalse();
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
                expect(() -> solver.newFreeVariables(SAT_SOLVER_MAX_CLAUSE_NUMBER + 1))
                    .toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClause(ImmutableIntList)", () -> {

            it("should work the same as #addClause(int...)", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-2);
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
            });

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(-1);
                expect(() -> solver.addClause(solver.newFreeVariables(1))).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClauseIf(int, ImmutableIntList)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseIf(int, int...)", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseIf(yes, solver.newFreeVariables(2));
                    solver.addClause(-3);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-2);
                    expectNoModelExists();
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
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(2));
                    solver.addClause(-4);
                    solver.addClause(-5);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(2));
                    expectNoModelExists();
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
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
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
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
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
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
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
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
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
                expectNoModelExists();
            });

            it("should ensure the literals be assigned the same value (the false case)", () -> {
                solver.addClause(1, 2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(-1);
                expectNoModelExists();
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
                expectNoModelExists();
            });

            it("should ensure the literals be assigned the same value (the false case)", () -> {
                solver.addClause(1, 2, 3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(-1);
                expectNoModelExists();
            });

            it("should throw an exception when the setting causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                solver.addClause(3);
                expect(() -> solver.markEachAsEquivalent(1, 2, 3)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#markAsGreaterEqualInBinary(ImmutableIntList, ImmutableIntList)", () -> {

            it("should encode the greater situation correctly (case 1)", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.setLiteralFalsy(bitArray1.get(1));
                solver.setLiteralTruthy(bitArray2.get(1));
                expectModelExists();
                expect(model().contains(bitArray1.get(0))).toBeTrue();
                expect(model().contains(bitArray2.get(0))).toBeFalse();
            });

            it("should encode the greater situation correctly (case 2)", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.setLiteralTruthy(bitArray2.get(0));
                expectModelExists();
                expect(model().contains(bitArray1.get(0))).toBeTrue();
            });

            it("should encode the greater situation correctly (case 3)", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.setLiteralFalsy(bitArray1.get(0));
                solver.setLiteralTruthy(bitArray2.get(0));
                expectNoModelExists();
            });

            it("should encode the equal situation correctly", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.markAsGreaterEqualInBinary(bitArray2, bitArray1);
                while (modelExists()) {
                    expect(model().contains(bitArray1.get(0))).toEqual(model().contains(bitArray2.get(0)));
                    expect(model().contains(bitArray1.get(1))).toEqual(model().contains(bitArray2.get(1)));
                    solver.addClauseBlocking(model());
                }
            });

            it("should handle array1 longer than array2 correctly", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(1);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                while (modelExists()) {
                    expect(model().containsAll(-bitArray1.get(0), -bitArray1.get(1), bitArray2.get(0))).toBeFalse();
                    solver.addClauseBlocking(model());
                }
            });

            it("should handle array1 shorter than array2 correctly", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(1);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                while (modelExists()) {
                    expect(model().contains(bitArray2.get(0))).toBeFalse();
                    expect(model().containsAll(-bitArray1.get(0), bitArray2.get(1))).toBeFalse();
                    solver.addClauseBlocking(model());
                }
            });

        });

        describe("#addClauseAtLeast(int, ImmutableIntList)", () -> {

            it("should work the same as #addClauseAtLeast(int, int...)", () -> {
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectModelExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
                expectNoModelExists();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                expectNoModelExists();
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
                    expectModelExists();
                    expect(model().containsAll(4, 5)).toBeTrue();

                    solver.addClause(-4);
                    expectNoModelExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtLeastIf(yes, 2, solver.newFreeVariables(4));
                    expectNoModelExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtLeastIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    solver.addClause(-4);
                    solver.addClause(-5);
                    solver.addClause(-6);
                    solver.addClause(-7);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtLeastIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    expectNoModelExists();
                });

            });

        });

        describe("#addClauseAtMost(ImmutableIntList)", () -> {

            it("should work the same as #addClauseAtMost(int...)", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectModelExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoModelExists();
            });

            it("should allow zero true assignments in the given clause", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(-3);
                solver.addClause(-4);
                expectModelExists();
                expect(model().containsAll(-1, -2, -3, -4)).toBeTrue();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                expectNoModelExists();
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
                    expectModelExists();
                    expect(model().containsAll(-4, -5)).toBeTrue();

                    solver.addClause(4);
                    expectNoModelExists();
                });

                it("should allow zero true assignments in the given clause", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtMostIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(-2);
                    solver.addClause(-3);
                    solver.addClause(-4);
                    solver.addClause(-5);
                    expectModelExists();
                    expect(model().containsAll(-2, -3, -4, -5)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseAtMostIf(yes, 2, solver.newFreeVariables(4));
                    expectNoModelExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtMostIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    solver.addClause(4);
                    solver.addClause(5);
                    solver.addClause(6);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseAtMostIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    expectNoModelExists();
                });

            });

        });

        describe("#addClauseExactly(int, ImmutableIntList)", () -> {

            it("should ensure no more than the given degree", () -> {
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectModelExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoModelExists();
            });

            it("should ensure no less than the given degree", () -> {
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectModelExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
                expectNoModelExists();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoModelExists();

                solver.addClauseExactly(2, solver.newFreeVariables(4));
                expectNoModelExists();
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
                    expectModelExists();
                    expect(model().containsAll(-4, -5)).toBeTrue();

                    solver.addClause(4);
                    expectNoModelExists();
                });

                it("should ensure no less than the given degree", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseExactlyIf(yes, 2, solver.newFreeVariables(4));
                    solver.addClause(-2);
                    solver.addClause(-3);
                    expectModelExists();
                    expect(model().containsAll(4, 5)).toBeTrue();

                    solver.addClause(-4);
                    expectNoModelExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseExactlyIf(yes, 2, solver.newFreeVariables(4));
                    expectNoModelExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseExactlyIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    solver.addClause(4);
                    solver.addClause(5);
                    solver.addClause(6);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseExactlyIf(wellNeverMind, 2, solver.newFreeVariables(4));
                    expectNoModelExists();
                });

            });

        });

        describe("#addClauseBlocking(ImmutableIntSet)", () -> {

            it("should prevent the given clause showing up as an model", () -> {
                solver.addClause(1, 2);
                solver.addClauseBlocking(IntSets.immutable.of(1, 2));
                solver.addClauseBlocking(IntSets.immutable.of(-1, 2));
                expectModelExists();
                expect(model().containsAll(1, -2)).toBeTrue();

                solver.addClause(2);
                expectNoModelExists();
            });

            it("can loop to keep blocking a problem until it is made unsatisfiable", () -> {
                solver.addClause(1, 2, 3);
                while (modelExists()) {
                    solver.addClauseBlocking(model());
                }
            });

        });

        describe("#addClauseBlockingIf(int, ImmutableIntSet)", () -> {

            describe("when activated", () -> {

                it("should work the same as #addClauseBlocking(ImmutableIntSet)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3);
                    solver.addClauseBlockingIf(1, IntSets.immutable.of(2, 3));
                    solver.addClauseBlockingIf(1, IntSets.immutable.of(-2, 3));
                    expectModelExists();
                    expect(model().containsAll(2, -3)).toBeTrue();

                    solver.addClause(3);
                    expectNoModelExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    solver.addClause(3);
                    solver.addClauseBlockingIf(3, IntSets.immutable.of(4, 5));
                    expectNoModelExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-3);
                    solver.addClauseBlockingIf(3, IntSets.immutable.of(4, 5));
                    solver.addClause(4);
                    solver.addClause(5);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    solver.addClause(-3);
                    solver.addClauseBlockingIf(3, IntSets.immutable.of(4, 5));
                    expectNoModelExists();
                });

            });

        });

        describe("#addImplications(int, int...)", () -> {

            it("should add the implication as expected (true antecedent)", () -> {
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(1);
                expectModelExists();
                expect(model().containsAll(2, 3)).toBeTrue();

                solver.addClause(-2);
                expectNoModelExists();
            });

            it("should add the implication as expected (false antecedent)", () -> {
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(3);
                expectModelExists();
            });

            it("should not affect the established satisfiability", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectModelExists();
                expect(model().contains(2)).toBeTrue();

                solver.addImplications(3, 4, 5);
                expectModelExists();
                expect(model().contains(2)).toBeTrue();
            });

            it("should not affect the established unsatisfiability", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoModelExists();

                solver.addImplications(3, 4, 5);
                expectNoModelExists();
            });

        });

        describe("#addImplicationsIf(int, int, int...)", () -> {

            describe("when activated", () -> {

                it("should add the implication as expected (true antecedent)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(2);
                    expectModelExists();
                    expect(model().containsAll(3, 4)).toBeTrue();

                    solver.addClause(-3);
                    expectNoModelExists();
                });

                it("should add the implication as expected (false antecedent)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(-2);
                    solver.addClause(-3);
                    solver.addClause(4);
                    expectModelExists();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    solver.addClause(3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    expectNoModelExists();
                });

            });

            describe("when not activated", () -> {

                it("should not affect the established satisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    solver.addClause(4);
                    solver.addClause(-5);
                    solver.addClause(6);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();
                });

                it("should not affect the established unsatisfiability", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    solver.addClause(-3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    expectNoModelExists();
                });

            });

        });
    }
}
