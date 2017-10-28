package core.proof;

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

            it("changes the verbosity (case 1)", () -> {
                solver.setVerbose(true);
                expect(solver.isVerbose()).toBeTrue();
                solver.setVerbose(false);
                expect(solver.isVerbose()).toBeFalse();
            });

            it("changes the verbosity (case 2)", () -> {
                solver.setVerbose(false);
                expect(solver.isVerbose()).toBeFalse();
                solver.setVerbose(true);
                expect(solver.isVerbose()).toBeTrue();
            });

        });

        describe("#get/setTimeout*()", () -> {

            it("changes the second timeout setting", () -> {
                final int t = solver.getTimeoutInSec();
                solver.setTimeoutInSec(12);
                expect(solver.getTimeoutInSec()).toEqual(12);
                solver.setTimeoutInSec(34);
                expect(solver.getTimeoutInSec()).toEqual(34);
                solver.setTimeoutInSec(t);
            });

            it("changes the millisecond timeout setting", () -> {
                final long t = solver.getTimeoutInMs();
                solver.setTimeoutInMs(1200);
                expect(solver.getTimeoutInMs()).toEqual(1200);
                solver.setTimeoutInMs(3400);
                expect(solver.getTimeoutInMs()).toEqual(3400);
                solver.setTimeoutInMs(t);
            });

        });

        describe("#addClauseAtLeast(int, int...)", () -> {

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(-1);
                solver.addClause(-2);
                solver.addClause(-3);
                expect(() -> solver.addClauseAtLeast(2, 1, 2, 3)).toThrow(Exception.class);
            });

        });

        describe("#addClauseAtMost(int, int...)", () -> {

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(1);
                solver.addClause(2);
                solver.addClause(3);
                expect(() -> solver.addClauseAtMost(2, 1, 2, 3)).toThrow(Exception.class);
            });

        });

        describe("#addClauseExactly(int, int...)", () -> {

            it("complains when it causes trivial contradictions", () -> {
                solver.addClause(1);
                solver.addClause(2);
                solver.addClause(3);
                expect(() -> solver.addClauseExactly(2, 1, 2, 3)).toThrow(Exception.class);
            });

        });

        describe("#findItSatisfiable()", () -> {

            it("returns true when SAT", () -> {
                solver.addClause(1, 2);
                final boolean satisfiable1 = solver.findItSatisfiable();
                expect(satisfiable1).toBeTrue();
                final boolean satisfiable2 = solver.findItSatisfiable();
                expect(satisfiable1).toEqual(satisfiable2);
            });

            it("returns false when UNSAT", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                final boolean satisfiable1 = solver.findItSatisfiable();
                expect(satisfiable1).toBeFalse();
                final boolean satisfiable2 = solver.findItSatisfiable();
                expect(satisfiable1).toEqual(satisfiable2);
            });

        });

        describe("#getModelTruthyVariables", () -> {

            it("returns the truthy variables in the model", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectModelExists();
                expect(solver.getModelTruthyVariables().contains(1)).toBeFalse();
                expect(solver.getModelTruthyVariables().contains(2)).toBeTrue();
            });

            it("complains when no model exists", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoModelExists();
                expect(() -> solver.getModelTruthyVariables()).toThrow(IllegalStateException.class);
            });

            it("complains if the problem is yet solved", () -> {
                expect(() -> solver.getModelTruthyVariables()).toThrow(IllegalStateException.class);
            });

        });

        describe("#getModelFalsyVariables", () -> {

            it("returns the falsy variables in the model", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                expectModelExists();
                expect(solver.getModelFalsyVariables().contains(1)).toBeTrue();
                expect(solver.getModelFalsyVariables().contains(2)).toBeFalse();
            });

            it("complains when no model exists", () -> {
                solver.addClause(1, 2);
                solver.addClause(-1);
                solver.addClause(-2);
                expectNoModelExists();
                expect(() -> solver.getModelFalsyVariables()).toThrow(IllegalStateException.class);
            });

            it("complains if the problem is yet solved", () -> {
                expect(() -> solver.getModelFalsyVariables()).toThrow(IllegalStateException.class);
            });

        });
    }
}
