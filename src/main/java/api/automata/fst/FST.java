package api.automata.fst;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.Automaton;
import api.automata.State;
import api.automata.fsa.FSA;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

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

        return Alphabets.create(alphabet().asSet().collect(Pair::getOne, set), alphabet().epsilon().getOne());
    }

    default Alphabet<T> outputAlphabet()
    {
        final MutableSet<T> set = UnifiedSet.newSet(alphabet().size()); // almost sure upper bound

        return Alphabets.create(alphabet().asSet().collect(Pair::getTwo, set), alphabet().epsilon().getTwo());
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

    FST<S, T> intersect(FST<S, T> target);

    FST<S, T> union(FST<S, T> target);

    FSA<Pair<S, T>> asFSA();
}
