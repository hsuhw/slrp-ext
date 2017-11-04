package core.proof;

import api.proof.ContradictionException;
import api.proof.SatSolver;
import org.eclipse.collections.api.list.primitive.ImmutableIntList;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.*;
import static core.util.Parameters.SAT_SOLVER_MAX_CLAUSE_NUMBER;

public abstract class AbstractSatSolverTest
{
    protected SatSolver solver;

    protected boolean modelExists()
    {
        return solver.findItSatisfiable();
    }

    protected void expectModelExists()
    {
        expect(solver.findItSatisfiable()).toBeTrue();
    }

    protected void expectNoModelExists()
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

            it("meets a minimum expectation", () -> {
                final ImmutableIntList interval = solver.newFreeVariables(3);
                expect(interval).toBeNotNull();
                expect(interval.size()).toEqual(3);
                expect(interval.containsAll(1, 2, 3)).toBeTrue();
            });

            it("provides a singleton when called with 1", () -> {
                final ImmutableIntList singletonInterval = solver.newFreeVariables(1);
                expect(singletonInterval).toBeNotNull();
                expect(singletonInterval.size()).toEqual(1);
                expect(singletonInterval.contains(1)).toBeTrue();
            });

            it("provides empty when called with 0", () -> {
                final ImmutableIntList empty = solver.newFreeVariables(0);
                expect(empty).toBeNotNull();
                expect(empty.isEmpty()).toBeTrue();
            });

            it("only accepts positive integer", () -> {
                expect(() -> solver.newFreeVariables(-1)).toThrow(IllegalArgumentException.class);
            });

            it("complains when the solver runs out of its variables", () -> {
                expect(() -> solver.newFreeVariables(SAT_SOLVER_MAX_CLAUSE_NUMBER + 1))
                    .toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClause(IntList)", () -> {

            it("is #addClause(int...)", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-2);
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(-1);
                expect(() -> solver.addClause(solver.newFreeVariables(1))).toThrow(Exception.class);
            });

        });

        describe("#addClauseIf(int, IntList)", () -> {

            describe("when activated", () -> {

                it("is #addClauseIf(int, int...)", () -> {
                    final int yes = solver.newFreeVariables(1).getFirst();
                    solver.addClause(yes);
                    solver.addClauseIf(yes, solver.newFreeVariables(2));
                    solver.addClause(-3);
                    expectModelExists();
                    expect(model().contains(2)).toBeTrue();

                    solver.addClause(-2);
                    expectNoModelExists();
                });

                it("complains when causing trivial contradictions", () -> {
                    expect(() -> {
                        final int yes = solver.newFreeVariables(1).getFirst();
                        solver.addClause(yes);
                        solver.addClause(-2);
                        solver.addClauseIf(yes, solver.newFreeVariables(1));
                    }).toThrow(ContradictionException.class);
                });

            });

            describe("when inactivated", () -> {

                it("does not affect SAT", () -> {
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

                it("does not affect UNSAT", () -> {
                    solver.addClause(solver.newFreeVariables(2));
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(2));
                    expectNoModelExists();
                });

                it("never causes a contradiction", () -> {
                    final int wellNeverMind = solver.newFreeVariables(1).getFirst();
                    solver.addClause(-wellNeverMind);
                    solver.addClause(-2);
                    solver.addClauseIf(wellNeverMind, solver.newFreeVariables(1));
                });

            });

        });

        describe("#setLiteralTruthy(int)", () -> {

            it("ensures the literal be assigned true in the model", () -> {
                solver.addClause(1, 2);
                solver.setLiteralTruthy(-2);
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(1);
                expect(() -> solver.setLiteralTruthy(-1)).toThrow(ContradictionException.class);
            });

        });

        describe("#setLiteralsTruthy(int...)", () -> {

            it("ensures the literals be assigned true in the model", () -> {
                solver.addClause(1, 2, 3);
                solver.setLiteralsTruthy(-2, -3);
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.setLiteralsTruthy(1, 2);
                expect(() -> solver.setLiteralsTruthy(-1, -2)).toThrow(ContradictionException.class);
            });

        });

        describe("#setLiteralFalsy(int)", () -> {

            it("ensures the literal be assigned false in the model", () -> {
                solver.addClause(1, 2);
                solver.setLiteralFalsy(2);
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(1);
                expect(() -> solver.setLiteralFalsy(1)).toThrow(ContradictionException.class);
            });

        });

        describe("#setLiteralsFalsy(int...)", () -> {

            it("ensures the literals be assigned false in the model", () -> {
                solver.addClause(1, 2, 3);
                solver.setLiteralsFalsy(2, 3);
                expectModelExists();
                expect(model().contains(1)).toBeTrue();

                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.setLiteralsFalsy(1, 2);
                expect(() -> solver.setLiteralsFalsy(-1, -2)).toThrow(ContradictionException.class);
            });

        });

        describe("#markAsEquivalent(int, int)", () -> {

            it("ensures the literals be assigned the same value (as true)", () -> {
                solver.addClause(-1, -2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(1);
                expectNoModelExists();
            });

            it("ensures the literals be assigned the same value (as false)", () -> {
                solver.addClause(1, 2);
                solver.markAsEquivalent(1, 2);
                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                expect(() -> solver.markAsEquivalent(1, 2)).toThrow(ContradictionException.class);
            });

        });

        describe("#markEachAsEquivalent(int...)", () -> {

            it("ensures the literals be assigned the same value (as true)", () -> {
                // same as true
                solver.addClause(-1, -2, -3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(1);
                expectNoModelExists();
            });

            it("ensures the literals be assigned the same value (as false)", () -> {
                solver.addClause(1, 2, 3);
                solver.markEachAsEquivalent(1, 2, 3);
                solver.addClause(-1);
                expectNoModelExists();
            });

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(1);
                solver.addClause(-2);
                solver.addClause(3);
                expect(() -> solver.markEachAsEquivalent(1, 2, 3)).toThrow(ContradictionException.class);
            });

        });

        describe("#markAsGreaterEqualInBinary(int[], int[])", () -> {

            it("complains when empty arrays given", () -> {
                expect(() -> solver.markAsGreaterEqualInBinary(new int[0], new int[0])).toThrow(Exception.class);
                expect(() -> solver.markAsGreaterEqualInBinary(new int[0], new int[1])).toThrow(Exception.class);
                expect(() -> solver.markAsGreaterEqualInBinary(new int[1], new int[0])).toThrow(Exception.class);
            });

        });

        describe("#markAsGreaterEqualInBinary(IntList, IntList)", () -> {

            it("encodes the greater situation 1", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.setLiteralFalsy(bitArray1.get(1));
                solver.setLiteralTruthy(bitArray2.get(1));
                expectModelExists();
                expect(model().contains(bitArray1.get(0))).toBeTrue();
                expect(model().contains(bitArray2.get(0))).toBeFalse();
            });

            it("encodes the greater situation 2", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.setLiteralTruthy(bitArray2.get(0));
                expectModelExists();
                expect(model().contains(bitArray1.get(0))).toBeTrue();
            });

            it("encodes the greater situation 3", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(2);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                solver.setLiteralFalsy(bitArray1.get(0));
                solver.setLiteralTruthy(bitArray2.get(0));
                expectNoModelExists();
            });

            it("encodes the equal situation", () -> {
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

            it("handles array1 longer than array2", () -> {
                final ImmutableIntList bitArray1 = solver.newFreeVariables(2);
                final ImmutableIntList bitArray2 = solver.newFreeVariables(1);
                solver.markAsGreaterEqualInBinary(bitArray1, bitArray2);
                while (modelExists()) {
                    expect(model().containsAll(-bitArray1.get(0), -bitArray1.get(1), bitArray2.get(0))).toBeFalse();
                    solver.addClauseBlocking(model());
                }
            });

            it("handles array1 shorter than array2", () -> {
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

        describe("#addClauseAtLeast(int, IntList)", () -> {

            it("is #addClauseAtLeast(int, int...)", () -> {
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectModelExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
                expectNoModelExists();
            });

            it("does not affect UNSAT", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtLeast(2, solver.newFreeVariables(4));
                expectNoModelExists();
            });

        });

        describe("#addClauseAtLeastIf(int, int, IntList)", () -> {

            describe("when activated", () -> {

                it("is #addClauseAtLeast(int, IntList)", () -> {
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

                it("does not affect UNSAT", () -> {
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

            describe("when inactivated", () -> {

                it("does not affect SAT", () -> {
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

                it("does not affect UNSAT", () -> {
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

        describe("#addClauseAtMost(IntList)", () -> {

            it("is #addClauseAtMost(int...)", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectModelExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoModelExists();
            });

            it("allows no true in the clause", () -> {
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(-3);
                solver.addClause(-4);
                expectModelExists();
                expect(model().containsAll(-1, -2, -3, -4)).toBeTrue();
            });

            it("does not affect UNSAT", () -> {
                solver.addClause(solver.newFreeVariables(2));
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClauseAtMost(2, solver.newFreeVariables(4));
                expectNoModelExists();
            });

        });

        describe("#addClauseAtMostIf(int, int, IntList)", () -> {

            describe("when activated", () -> {

                it("is #addClauseAtMost(int, IntList)", () -> {
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

                it("allows no true in the clause", () -> {
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

                it("does not affect UNSAT", () -> {
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

            describe("when inactivated", () -> {

                it("does not affect SAT", () -> {
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

                it("does not affect UNSAT", () -> {
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

        describe("#addClauseExactly(int, IntList)", () -> {

            it("ensures no-more-than the degree", () -> {
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(1);
                solver.addClause(2);
                expectModelExists();
                expect(model().containsAll(-3, -4)).toBeTrue();

                solver.addClause(3);
                expectNoModelExists();
            });

            it("ensures no-less-than the degree", () -> {
                solver.addClauseExactly(2, solver.newFreeVariables(4));
                solver.addClause(-1);
                solver.addClause(-2);
                expectModelExists();
                expect(model().containsAll(3, 4)).toBeTrue();

                solver.addClause(-3);
                expectNoModelExists();
            });

            it("does not affect UNSAT", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoModelExists();

                solver.addClauseExactly(2, solver.newFreeVariables(4));
                expectNoModelExists();
            });

        });

        describe("#addClauseExactlyIf(int, int, IntList)", () -> {

            describe("when activated", () -> {

                it("ensures no-more-than the degree", () -> {
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

                it("ensures no-less-than the degree", () -> {
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

                it("does not affect UNSAT", () -> {
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

            describe("when inactivated", () -> {

                it("does not affect SAT", () -> {
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

                it("does not affect UNSAT", () -> {
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

        describe("#addClauseBlocking(IntSet)", () -> {

            it("prevents the clause showing up as an model", () -> {
                solver.addClause(1, 2);
                solver.addClauseBlocking(IntSets.immutable.of(1, 2));
                solver.addClauseBlocking(IntSets.immutable.of(-1, 2));
                expectModelExists();
                expect(model().containsAll(1, -2)).toBeTrue();

                solver.addClause(2);
                expectNoModelExists();
            });

            it("can keep blocking a problem till it's UNSAT", () -> {
                solver.addClause(1, 2, 3);
                while (modelExists()) {
                    solver.addClauseBlocking(model());
                }
            });

        });

        describe("#addClauseBlockingIf(int, IntSet)", () -> {

            describe("when activated", () -> {

                it("is #addClauseBlocking(IntSet)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3);
                    solver.addClauseBlockingIf(1, IntSets.immutable.of(2, 3));
                    solver.addClauseBlockingIf(1, IntSets.immutable.of(-2, 3));
                    expectModelExists();
                    expect(model().containsAll(2, -3)).toBeTrue();

                    solver.addClause(3);
                    expectNoModelExists();
                });

                it("does not affect UNSAT", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    solver.addClause(3);
                    solver.addClauseBlockingIf(3, IntSets.immutable.of(4, 5));
                    expectNoModelExists();
                });

            });

            describe("when inactivated", () -> {

                it("does not affect SAT", () -> {
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

                it("does not affect UNSAT", () -> {
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

            it("adds the implication (true antecedent)", () -> {
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(1);
                expectModelExists();
                expect(model().containsAll(2, 3)).toBeTrue();

                solver.addClause(-2);
                expectNoModelExists();
            });

            it("adds the implication (false antecedent)", () -> {
                solver.addClause(1, 2, 3, 4);
                solver.addImplications(1, 2, 3);
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(3);
                expectModelExists();
            });

            it("does not affect SAT", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectModelExists();
                expect(model().contains(2)).toBeTrue();

                solver.addImplications(3, 4, 5);
                expectModelExists();
                expect(model().contains(2)).toBeTrue();
            });

            it("does not affect UNSAT", () -> {
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

                it("adds the implication (true antecedent)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(2);
                    expectModelExists();
                    expect(model().containsAll(3, 4)).toBeTrue();

                    solver.addClause(-3);
                    expectNoModelExists();
                });

                it("adds the implication (false antecedent)", () -> {
                    solver.addClause(1);
                    solver.addClause(2, 3, 4, 5);
                    solver.addImplicationsIf(1, 2, 3, 4);
                    solver.addClause(-2);
                    solver.addClause(-3);
                    solver.addClause(4);
                    expectModelExists();
                });

                it("does not affect UNSAT", () -> {
                    solver.addClause(1, 2);
                    solver.addClause(-1);
                    solver.addClause(-2);
                    expectNoModelExists();

                    solver.addClause(3);
                    solver.addImplicationsIf(3, 4, 5, 6);
                    expectNoModelExists();
                });

            });

            describe("when inactivated", () -> {

                it("does not affect SAT", () -> {
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

                it("does not affect UNSAT", () -> {
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
