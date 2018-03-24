package api.automata.fst;

import api.automata.*;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.LinkedList;
import java.util.List;

import static api.util.Connectives.*;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface FST<S, T> extends Automaton<Pair<S, T>>
{
    @Override
    FST<S, T> trimUnreachableStates();

    @Override
    default boolean isDeterministic()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    @Override
    MutableFST<S, T> toMutable();

    @Override
    default ImmutableFST<S, T> toImmutable()
    {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_YET);
    }

    default Alphabet<S> inputAlphabet()
    {
        final MutableSet<S> set = UnifiedSet.newSet(alphabet().size()); // almost sure upper bound
        final S epsilon = alphabet().epsilon().getOne();
        set.add(epsilon);

        return Alphabets.create(alphabet().asSet().collect(Pair::getOne, set), epsilon);
    }

    default Alphabet<T> outputAlphabet()
    {
        final MutableSet<T> set = UnifiedSet.newSet(alphabet().size()); // almost sure upper bound
        final T epsilon = alphabet().epsilon().getTwo();
        set.add(epsilon);

        return Alphabets.create(alphabet().asSet().collect(Pair::getTwo, set), epsilon);
    }

    default FSA<S> domain()
    {
        return (FSA<S>) project(inputAlphabet(), Pair::getOne);
    }

    default FSA<T> range()
    {
        return (FSA<T>) project(outputAlphabet(), Pair::getTwo);
    }

    default FST<T, S> inverse()
    {
        final Alphabet<Pair<T, S>> inverseAlphabet = Alphabets.product(outputAlphabet(), inputAlphabet());

        return (FST<T, S>) project(inverseAlphabet, Labels.flipped());
    }

    default FST<S, T> maskByInput(FSA<S> mask)
    {
        return (FST<S, T>) product(mask, alphabet(), Labels.inputMatched(), AcceptStates.select(this, mask, AND));
    }

    default FST<S, T> maskByOutput(FSA<T> mask)
    {
        return (FST<S, T>) product(mask, alphabet(), Labels.outputMatched(), AcceptStates.select(this, mask, AND));
    }

    default <U> FST<S, U> compose(FST<T, U> target, Alphabet<Pair<S, U>> alphabet)
    {
        return (FST<S, U>) product(target, alphabet, Labels.composed(), AcceptStates.select(this, target, AND));
    }

    private SetIterable<MutableStack<T>> run(State<Pair<S, T>> state, ListIterable<S> inputNoAnyEpsilon, int capacity)
    {
        final int inputSize = inputNoAnyEpsilon.size();
        if (inputSize == 0) {
            return isAcceptState(state) ? Sets.immutable.of(new ArrayStack<>(capacity)) : Sets.immutable.empty();
        }

        final int resultCapacity = inputSize * inputSize; // heuristic
        final MutableSet<MutableStack<T>> result = UnifiedSet.newSet(resultCapacity);
        final Pair<S, T> epsilon = alphabet().epsilon();

        SetIterable<MutableStack<T>> postfixImages;
        boolean isEpsilonStep;
        for (Pair<S, T> inOut : state.enabledSymbols()) {
            if ((isEpsilonStep = inOut.equals(epsilon)) || inOut.getOne().equals(inputNoAnyEpsilon.get(0))) {
                for (State<Pair<S, T>> succ : state.successors(inOut)) {
                    final ListIterable<S> postfix = isEpsilonStep
                                                    ? inputNoAnyEpsilon
                                                    : inputNoAnyEpsilon.subList(1, inputNoAnyEpsilon.size());
                    postfixImages = run(succ, postfix, capacity);
                    for (MutableStack<T> postfixOutput : postfixImages) {
                        if (!isEpsilonStep) {
                            postfixOutput.push(inOut.getTwo());
                        }
                        result.add(postfixOutput);
                    }
                }
            }
        }

        return result;
    }

    default RichIterable<ListIterable<T>> postImage(ListIterable<S> word)
    {
        final ListIterable<S> trimmedWord = word.allSatisfy(inputAlphabet()::notEpsilon)
                                            ? word
                                            : word.select(inputAlphabet()::notEpsilon);

        return run(startState(), trimmedWord, trimmedWord.size()).collect(each -> (ListIterable<T>) each.toList());
    }

    default FSA<T> postImage(FSA<S> fsa)
    {
        return (FSA<T>) product(fsa, outputAlphabet(), Labels.transduced(), AcceptStates.select(this, fsa, AND));
    }

    private static <S> ObjectIntPair<FSA<S>> postStarImageOnLength(FST<S, S> fst, FSA<S> target, int length)
    {
        final var trimmedTarget = target.intersect(FSAs.acceptingAllOnLength(target.alphabet(), length));

        final List<FSA<S>> closedImages = new LinkedList<>();
        closedImages.add(trimmedTarget);
        var currTarget = trimmedTarget;
        var convergeSteps = 0;
        while (true) {
            convergeSteps++;
            var remains = fst.postImage(currTarget);
            for (var closedImage : closedImages) {
                final var containment = closedImage.checkContainingWithCounterSource(remains);
                if (containment.passed()) { // union all closed images
                    final var stateCapacity = closedImages.stream().mapToInt(each -> each.states().size()).sum() + 1;
                    final var result = FSAs.create(target.alphabet(), stateCapacity);
                    final var start = result.startState();
                    closedImages.forEach(image -> {
                        @SuppressWarnings("unchecked")
                        final SetIterable<MutableState<S>> states = (SetIterable) image.states();
                        @SuppressWarnings("unchecked")
                        final SetIterable<MutableState<S>> accepts = (SetIterable) image.acceptStates();
                        result.addStates(states).addEpsilonTransition(start, (MutableState<S>) image.startState())
                              .setAllAsAccept(accepts);
                    });
                    return PrimitiveTuples.pair(result, convergeSteps);
                }
                remains = containment.counterexample().sourceImage();
            }
            closedImages.add(remains);
            currTarget = remains;
        }
    }

    default ObjectIntPair<FSA<S>> postStarImageOnLength(FSA<S> target, int length)
    {
        final var epsilon = alphabet().epsilon();
        final var input = epsilon.getOne();
        final var output = epsilon.getTwo();
        if (!input.getClass().isInstance(output) || !output.getClass().isInstance(input)) {
            throw new UnsupportedOperationException("only available on same space mapping instances");
        }

        @SuppressWarnings("unchecked")
        final var fst = (FST<S, S>) this;
        return postStarImageOnLength(fst, target, length);
    }

    default RichIterable<ListIterable<S>> preImage(ListIterable<T> word)
    {
        final ListIterable<T> trimmedWord = word.allSatisfy(outputAlphabet()::notEpsilon)
                                            ? word
                                            : word.select(outputAlphabet()::notEpsilon);
        final FST<T, S> inv = inverse(); // should be cached

        return inv.run(inv.startState(), trimmedWord, trimmedWord.size())
                  .collect(each -> (ListIterable<S>) each.toList());
    }

    default FSA<S> preImage(FSA<T> fsa)
    {
        final FST<T, S> inv = inverse(); // should be cached

        return (FSA<S>) inv.product(fsa, inputAlphabet(), Labels.transduced(), AcceptStates.select(inv, fsa, AND));
    }

    default ObjectIntPair<FSA<S>> preStarImageOnLength(FSA<S> target, int length)
    {
        final var epsilon = alphabet().epsilon();
        final var input = epsilon.getOne();
        final var output = epsilon.getTwo();
        if (!input.getClass().isInstance(output) || !output.getClass().isInstance(input)) {
            throw new UnsupportedOperationException("only available on same space mapping instances");
        }

        @SuppressWarnings("unchecked")
        final var fst = (FST<S, S>) this;
        return postStarImageOnLength(fst.inverse(), target, length);
    }

    FST<S, T> intersect(FST<S, T> target);

    FST<S, T> union(FST<S, T> target);

    FSA<Pair<S, T>> asFSA();
}
