package core.proof;

import api.automata.Alphabet;
import api.automata.State;
import api.automata.TransitionGraph;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.stack.mutable.ArrayStack;

import static api.automata.AutomatonManipulator.selectFrom;
import static api.util.Connectives.AND;
import static api.util.Connectives.Labels;
import static core.util.Parameters.estimateExtendedSize;

public final class Transducers
{
    static <S, T, X extends Pair<S, T>, Y extends Pair<T, S>> FSA<Twin<S>> compose(FSA<X> one, FSA<Y> two,
                                                                                   Alphabet<Twin<S>> alphabet)
    {
        return FSAs.product(one, two, alphabet, Labels.composable(), (stateMapping, builder) -> {
            builder.addStartStates(selectFrom(stateMapping, one::isStartState, AND, two::isStartState));
            builder.addAcceptStates(selectFrom(stateMapping, one::isAcceptState, AND, two::isAcceptState));
        });
    }

    static <S, T, U extends Pair<S, T>> FSA<U> filterByInput(FSA<U> target, FSA<S> filter)
    {
        return FSAs.product(target, filter, target.alphabet(), Labels.whoseInputMatched(), (stateMapping, builder) -> {
            builder.addStartStates(selectFrom(stateMapping, target::isStartState, AND, filter::isStartState));
            builder.addAcceptStates(selectFrom(stateMapping, target::isAcceptState, AND, filter::isAcceptState));
        });
    }

    private static <S, T, U extends Pair<S, T>> SetIterable<MutableStack<S>> preImageAt(FSA<U> transducer, State state,
                                                                                        ImmutableList<T> word,
                                                                                        int capacity)
    {
        if (word.size() == 0) {
            return transducer.isAcceptState(state)
                   ? Sets.immutable.of(new ArrayStack<>(capacity))
                   : Sets.immutable.empty();
        }

        final TransitionGraph<State, U> delta = transducer.transitionGraph();
        final int resultCapacity = estimateExtendedSize(word.size() * word.size()); // heuristic
        final MutableSet<MutableStack<S>> result = UnifiedSet.newSet(resultCapacity);
        final U epsilonTrans = transducer.alphabet().epsilon();

        SetIterable<MutableStack<S>> postfixImages;
        boolean isEpsilonTrans;
        for (U trans : delta.arcLabelsFrom(state)) {
            if ((isEpsilonTrans = trans.equals(epsilonTrans)) || trans.getTwo().equals(word.get(0))) {
                for (State dest : delta.directSuccessorsOf(state, trans)) {
                    final ImmutableList<T> postfix = isEpsilonTrans ? word : word.subList(1, word.size());
                    postfixImages = preImageAt(transducer, dest, postfix, capacity);
                    for (MutableStack<S> postfixImage : postfixImages) {
                        postfixImage.push(trans.getOne());
                        result.add(postfixImage);
                    }
                }
            }
        }

        return result;
    }

    static <S, T> ImmutableSet<ImmutableList<S>> preImage(FSA<? extends Pair<S, T>> transducer, ImmutableList<T> word)
    {
        return preImageAt(transducer, transducer.startState(), word, word.size())
            .collect(each -> each.toList().toImmutable()).toSet().toImmutable();
    }

    private static <S, T, U extends Pair<S, T>> SetIterable<MutableStack<T>> postImageAt(FSA<U> transducer, State state,
                                                                                         ImmutableList<S> word,
                                                                                         int capacity)
    {
        if (word.size() == 0) {
            return transducer.isAcceptState(state)
                   ? Sets.immutable.of(new ArrayStack<>(capacity))
                   : Sets.immutable.empty();
        }

        final TransitionGraph<State, U> delta = transducer.transitionGraph();
        final int resultCapacity = estimateExtendedSize(word.size() * word.size()); // heuristic
        final MutableSet<MutableStack<T>> result = UnifiedSet.newSet(resultCapacity);
        final U epsilonTrans = transducer.alphabet().epsilon();

        SetIterable<MutableStack<T>> postfixImages;
        boolean isEpsilonTrans;
        for (U trans : delta.arcLabelsFrom(state)) {
            if ((isEpsilonTrans = trans.equals(epsilonTrans)) || trans.getOne().equals(word.get(0))) {
                for (State dest : delta.directSuccessorsOf(state, trans)) {
                    final ImmutableList<S> postfix = isEpsilonTrans ? word : word.subList(1, word.size());
                    postfixImages = postImageAt(transducer, dest, postfix, capacity);
                    for (MutableStack<T> postImage : postfixImages) {
                        if (!isEpsilonTrans) {
                            postImage.push(trans.getTwo());
                        }
                        result.add(postImage);
                    }
                }
            }
        }

        return result;
    }

    static <S, T> ImmutableSet<ImmutableList<T>> postImage(FSA<? extends Pair<S, T>> transducer, ImmutableList<S> word)
    {
        return postImageAt(transducer, transducer.startState(), word, word.size())
            .collect(each -> each.toList().toImmutable()).toSet().toImmutable();
    }

    static <S> FSA<S> postImage(FSA<Twin<S>> transducer, FSA<S> fsa)
    {
        return FSAs.product(transducer, fsa, fsa.alphabet(), Labels.transducible(), (stateMapping, builder) -> {
            builder.addStartStates(selectFrom(stateMapping, transducer::isStartState, AND, fsa::isStartState));
            builder.addAcceptStates(selectFrom(stateMapping, transducer::isAcceptState, AND, fsa::isAcceptState));
        });
    }
}
