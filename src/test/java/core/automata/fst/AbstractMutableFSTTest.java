package core.automata.fst;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.fsa.FSAs;
import api.automata.fst.MutableFST;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

abstract class AbstractMutableFSTTest
{
    private final Alphabet<Object> alphabet;
    private final Alphabet<Pair<Object, Object>> ioAlphabet;

    abstract <S, T> MutableFST<S, T> newFST(Alphabet<Pair<S, T>> alphabet, int stateCapacity);

    {
        final var e = new Object();
        final var a = new Object();
        final var b = new Object();
        final var ee = Tuples.pair(e, e);
        final var aa = Tuples.pair(a, a);
        final var ab = Tuples.pair(a, b);
        final var ba = Tuples.pair(b, a);
        final var bb = Tuples.pair(b, b);
        alphabet = Alphabets.builder(3, e).add(a).add(b).build();
        ioAlphabet = Alphabets.product(alphabet, alphabet);

        describe("#postStarImageOnLength", () -> {

            it("converge on simple cases", () -> {
                final var fsa = FSAs.create(alphabet, 2); // which accepts (ab)*
                final var s0 = fsa.startState();
                final var s1 = fsa.newState();
                fsa.addTransition(s0, s1, a);
                fsa.addTransition(s1, s0, b);
                fsa.setAsAccept(s0);
                final var fst = newFST(ioAlphabet, 3); // which push the first 'a' backward
                final var ts0 = fst.startState();
                final var ts1 = fst.newState();
                final var ts2 = fst.newState();
                fst.addTransition(ts0, ts0, aa);
                fst.addTransition(ts0, ts1, ba);
                fst.addTransition(ts1, ts2, ab);
                fst.addTransition(ts1, ts2, bb);
                fst.addTransition(ts2, ts2, aa);
                fst.addTransition(ts2, ts2, bb);
                fst.setAsAccept(ts2);

                final var resultWithStepCount1 = fst.postStarImageOnLength(fsa, 4);
                final var result1 = resultWithStepCount1.getOne();
                final var stepCount1 = resultWithStepCount1.getTwo();
                expect(result1.accepts(Lists.immutable.of(a, b, a, b))).toBeTrue();
                expect(result1.accepts(Lists.immutable.of(a, a, b, b))).toBeTrue();
                expect(result1.accepts(Lists.immutable.of(a, a, a, b))).toBeTrue();
                expect(stepCount1).toEqual(3);

                final var resultWithStepCount2 = fst.postStarImageOnLength(fsa, 7);
                final var result2 = resultWithStepCount2.getOne();
                final var stepCount2 = resultWithStepCount2.getTwo();
                expect(result2.acceptsNone()).toBeTrue();
                expect(stepCount2).toEqual(1);
            });

            it("converge with circular, nondeterministic transducers", () -> {
                final var fsa = FSAs.create(alphabet, 2); // which accepts (ab)*
                final var s0 = fsa.startState();
                final var s1 = fsa.newState();
                fsa.addTransition(s0, s1, a);
                fsa.addTransition(s1, s0, b);
                fsa.setAsAccept(s0);
                final var fst = newFST(ioAlphabet, 3); // which push the first 'a' backward, circularly
                final var ts0 = fst.startState();
                final var ts11 = fst.newState();
                final var ts12 = fst.newState();
                final var ts13 = fst.newState();
                final var ts21 = fst.newState();
                final var ts22 = fst.newState();
                final var ts23 = fst.newState();
                fst.addTransition(ts0, ts11, aa);
                fst.addTransition(ts0, ts12, ba);
                fst.addTransition(ts11, ts11, aa);
                fst.addTransition(ts11, ts12, ba);
                fst.addTransition(ts12, ts13, ab);
                fst.addTransition(ts12, ts13, bb);
                fst.addTransition(ts13, ts13, aa);
                fst.addTransition(ts13, ts13, bb);
                fst.addTransition(ts0, ts22, bb);
                fst.addTransition(ts0, ts22, ab);
                fst.addTransition(ts21, ts22, bb);
                fst.addTransition(ts21, ts22, ab);
                fst.addTransition(ts22, ts22, aa);
                fst.addTransition(ts22, ts22, bb);
                fst.addTransition(ts22, ts23, ba);
                fst.setAsAccept(ts13);
                fst.setAsAccept(ts23);

                final var resultWithStepCount = fst.postStarImageOnLength(fsa, 4);
                final var result = resultWithStepCount.getOne();
                final var stepCount = resultWithStepCount.getTwo();
                expect(result.accepts(Lists.immutable.of(a, b, a, b))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(b, a, b, a))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(b, b, a, a))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(a, b, b, a))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(a, a, b, b))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(b, a, a, a))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(a, b, a, a))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(a, a, b, a))).toBeTrue();
                expect(result.accepts(Lists.immutable.of(a, a, a, b))).toBeTrue();
                expect(stepCount).toEqual(4);
                expect(result.accepts(Lists.immutable.of(a, a, a, a))).toBeFalse();
                expect(result.accepts(Lists.immutable.of(b, b, b, b))).toBeFalse();
                expect(result.accepts(Lists.immutable.of(a, b, b, b))).toBeFalse();
                expect(result.accepts(Lists.immutable.of(b, a, b, b))).toBeFalse();
                expect(result.accepts(Lists.immutable.of(b, b, a, b))).toBeFalse();
                expect(result.accepts(Lists.immutable.of(b, b, b, a))).toBeFalse();
            });

        });
    }
}
