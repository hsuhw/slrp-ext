package api.automata.fst;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.Automaton;
import api.automata.State;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.VATA;
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

import static api.util.Connectives.*;
import static common.util.Constants.NOT_IMPLEMENTED_YET;

public interface FST<S, T> extends Automaton<Pair<S, T>>
{
    @Override
    FST<S, T> trimUnreachableStates();

    @Override
    FST<S, T> trimEpsilonTransitions();

    @Override
    FST<S, T> minimize();

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
        final var epsilon = alphabet().epsilon().getOne();
        set.add(epsilon);

        return Alphabets.create(alphabet().asSet().collect(Pair::getOne, set), epsilon);
    }

    default Alphabet<T> outputAlphabet()
    {
        final MutableSet<T> set = UnifiedSet.newSet(alphabet().size()); // almost sure upper bound
        final var epsilon = alphabet().epsilon().getTwo();
        set.add(epsilon);

        return Alphabets.create(alphabet().asSet().collect(Pair::getTwo, set), epsilon);
    }

    default boolean isSameSpaceMapping()
    {
        final var input = alphabet().epsilon().getOne();
        final var output = alphabet().epsilon().getTwo();

        return input.getClass().isInstance(output) && output.getClass().isInstance(input);
    }

    default FSA<S> domain()
    {
        return (FSA<S>) project(inputAlphabet(), Pair::getOne);
    }

    default FSA<T> range()
    {
        return (FSA<T>) project(outputAlphabet(), Pair::getTwo);
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
        final var inputSize = inputNoAnyEpsilon.size();
        if (inputSize == 0) {
            return isAcceptState(state) ? Sets.immutable.of(new ArrayStack<>(capacity)) : Sets.immutable.empty();
        }

        final var resultCapacity = inputSize * inputSize; // heuristic
        final MutableSet<MutableStack<T>> result = UnifiedSet.newSet(resultCapacity);
        final var epsilon = alphabet().epsilon();

        boolean isEpsilonStep;
        for (var inOutAndDest : state.transitions()) {
            final var inOut = inOutAndDest.getOne();
            if (!(isEpsilonStep = inOut.equals(epsilon)) && !inOut.getOne().equals(inputNoAnyEpsilon.get(0))) {
                continue;
            }
            final var postfix = isEpsilonStep
                                ? inputNoAnyEpsilon
                                : inputNoAnyEpsilon.subList(1, inputNoAnyEpsilon.size());
            for (var postfixOutput : run(inOutAndDest.getTwo(), postfix, capacity)) {
                if (!isEpsilonStep) {
                    postfixOutput.push(inOut.getTwo());
                }
                result.add(postfixOutput);
            }
        }

        return result;
    }

    default ListIterable<ListIterable<T>> postImage(ListIterable<S> word)
    {
        final var trimmedWord = word.allSatisfy(inputAlphabet()::notEpsilon)
                                ? word
                                : word.select(inputAlphabet()::notEpsilon);

        return run(startState(), trimmedWord, trimmedWord.size()).collect(each -> (ListIterable<T>) each.toList())
                                                                 .toList();
    }

    default FSA<T> postImage(FSA<S> fsa)
    {
        return (FSA<T>) product(fsa, outputAlphabet(), Labels.transduced(), AcceptStates.select(this, fsa, AND));
    }

    private static <S> ObjectIntPair<FSA<S>> postStarImageOnLength(FST<S, S> fst, FSA<S> target, int length)
    {
        if (fst.hasEpsilonTransitions() || target.hasEpsilonTransitions()) {
            throw new UnsupportedOperationException("only available without epsilon transitions");
        }

        final var trimmedTarget = target.intersect(FSAs.acceptingAllOnLength(target.alphabet(), length));
        var convergeSteps = 0;
        var currImage = trimmedTarget;

        while (true) {
            convergeSteps++;
            final var postImage = fst.postImage(currImage);
            if (VATA.checkInclusion(postImage, currImage)) {
                return PrimitiveTuples.pair(currImage, convergeSteps);
            }
            currImage = VATA.reduce(currImage.union(postImage));
        }
    }

    default ObjectIntPair<FSA<S>> postStarImageOnLength(FSA<S> target, int length)
    {
        if (!isSameSpaceMapping()) {
            throw new UnsupportedOperationException("only available on same space mapping instances");
        }

        @SuppressWarnings("unchecked")
        final FST<S, S> fst = (FST) this;
        return postStarImageOnLength(fst, target, length);
    }

    default ListIterable<ListIterable<S>> preImage(ListIterable<T> word)
    {
        final var trimmedWord = word.allSatisfy(outputAlphabet()::notEpsilon)
                                ? word
                                : word.select(outputAlphabet()::notEpsilon);
        final var inv = inverse(); // should be cached

        return inv.run(inv.startState(), trimmedWord, trimmedWord.size())
                  .collect(each -> (ListIterable<S>) each.toList()).toList();
    }

    default FSA<S> preImage(FSA<T> fsa)
    {
        final var inv = inverse(); // should be cached

        return (FSA<S>) inv.product(fsa, inputAlphabet(), Labels.transduced(), AcceptStates.select(inv, fsa, AND));
    }

    default ObjectIntPair<FSA<S>> preStarImageOnLength(FSA<S> target, int length)
    {
        if (!isSameSpaceMapping()) {
            throw new UnsupportedOperationException("only available on same space mapping instances");
        }

        @SuppressWarnings("unchecked")
        final FST<S, S> fst = (FST) this;
        return postStarImageOnLength(fst.inverse(), target, length);
    }

    default FST<T, S> inverse()
    {
        final var inverseAlphabet = Alphabets.product(outputAlphabet(), inputAlphabet());

        return (FST<T, S>) project(inverseAlphabet, Labels.flipped());
    }

    FST<S, T> intersect(FST<S, T> target);

    FST<S, T> union(FST<S, T> target);

    FSA<Pair<S, T>> asFSA();
}
