package core.synth;

import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

@RunWith(OleasterRunner.class)
public class Sat4jSolverAdapterTest extends AbstractSatSolverTest
{
    {
        solver = new Sat4jSolverAdapter();

        describe("#isVerbose() & #setVerbose()", () -> {

            it("should change the verbosity of the solver consistently (case 1)", () -> {
                solver.setVerbose(true);
                expect(solver.isVerbose()).toBeTrue();
                solver.setVerbose(false);
                expect(solver.isVerbose()).toBeFalse();
            });

            it("should change the verbosity of the solver consistently (case 2)", () -> {
                solver.setVerbose(false);
                expect(solver.isVerbose()).toBeFalse();
                solver.setVerbose(true);
                expect(solver.isVerbose()).toBeTrue();
            });

        });

        describe("#get/setTimeout*()", () -> {

            it("should change the timeout setting of the solver in seconds", () -> {
                solver.setTimeoutInSec(12);
                expect(solver.getTimeoutInSec()).toEqual(12);
                solver.setTimeoutInSec(34);
                expect(solver.getTimeoutInSec()).toEqual(34);
            });

            it("should change the timeout setting of the solver in milliseconds", () -> {
                solver.setTimeoutInMs(1200);
                expect(solver.getTimeoutInMs()).toEqual(1200);
                solver.setTimeoutInMs(3400);
                expect(solver.getTimeoutInMs()).toEqual(3400);
            });

        });

        describe("#addClauseAtLeast(int, int...)", () -> {

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(-3);
                expect(() -> solver.addClauseAtLeast(2, 1, 2, 3)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClauseAtMost(int, int...)", () -> {

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(2);
                solver.addClause(3);
                expect(() -> solver.addClauseAtMost(2, 1, 2, 3)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#addClauseExactly(int, int...)", () -> {

            it("should throw an exception when the clause causes an immediate contradiction", () -> {
                solver.addClause(1);
                solver.addClause(2);
                solver.addClause(3);
                expect(() -> solver.addClauseExactly(2, 1, 2, 3)).toThrow(IllegalArgumentException.class);
            });

        });

        describe("#findItSatisfiable()", () -> {

            it("should return positive consistently when a problem is satisfiable", () -> {
                solver.addClause(1, 2);
                final Boolean satisfiable1 = solver.findItSatisfiable();
                expect(satisfiable1).toBeNotNull();
                expect(satisfiable1.booleanValue()).toBeTrue();
                final Boolean satisfiable2 = solver.findItSatisfiable();
                expect(satisfiable1).toEqual(satisfiable2);
            });

            it("should return negative consistently when a problem is unsatisfiable", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                final Boolean satisfiable1 = solver.findItSatisfiable();
                expect(satisfiable1).toBeNotNull();
                expect(satisfiable1.booleanValue()).toBeFalse();
                final Boolean satisfiable2 = solver.findItSatisfiable();
                expect(satisfiable1).toEqual(satisfiable2);
            });

        });

        describe("#getModelTruthyVariables", () -> {

            it("should provide the truthy variables in the model", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectSolutionExists();

                expect(solver.getModelTruthyVariables().contains(1)).toBeFalse();
                expect(solver.getModelTruthyVariables().contains(2)).toBeTrue();
            });

            it("should throw an exception when called if there is no model to be found", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expect(() -> solver.getModelTruthyVariables()).toThrow(IllegalStateException.class);
            });

            it("should throw an exception when called if the problem has not been solved", () -> {
                expect(() -> solver.getModelTruthyVariables()).toThrow(IllegalStateException.class);
            });

        });

        describe("#getModelFalsyVariables", () -> {

            it("should provide the falsy variables in the model", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectSolutionExists();

                expect(solver.getModelFalsyVariables().contains(1)).toBeTrue();
                expect(solver.getModelFalsyVariables().contains(2)).toBeFalse();
            });

            it("should throw an exception when called if there is no model to be found", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expect(() -> solver.getModelFalsyVariables()).toThrow(IllegalStateException.class);
            });

            it("should throw an exception when called if the problem has not been solved", () -> {
                expect(() -> solver.getModelFalsyVariables()).toThrow(IllegalStateException.class);
            });

        });
    }
}
