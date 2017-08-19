package api.automata.fsa;

import api.automata.Automaton;
import api.automata.Manipulator;
import api.automata.Symbol;

import java.util.function.BiFunction;

public interface FSAManipulator extends Manipulator
{
    <S extends Symbol> FSA<S> determinize(FSA<S> target);

    <S extends Symbol> FSA<S> makeComplete(FSA<S> target);

    <S extends Symbol> FSA<S> minimize(FSA<S> target);

    <S extends Symbol> FSA<S> getComplement(FSA<S> target);

    interface Decorator extends FSAManipulator
    {
        FSAManipulator getDecoratee();

        <S extends Symbol> FSA<S> trimUnreachableStatesDelegated(Automaton<S> target);

        @Override
        default <S extends Symbol> FSA<S> trimUnreachableStates(Automaton<S> target)
        {
            final FSA<S> delegated = trimUnreachableStatesDelegated(target);
            if (delegated == null) {
                getDecoratee().trimUnreachableStates(target);
            }
            return delegated;
        }

        <S extends Symbol, T extends Symbol, R extends Symbol> FSA<S> composeDelegated(Automaton<S> first,
                                                                                       Automaton<T> after,
                                                                                       BiFunction<S, T, R> composer);

        @Override
        default <S extends Symbol, T extends Symbol, R extends Symbol> FSA<S> compose(Automaton<S> first,
                                                                                      Automaton<T> after,
                                                                                      BiFunction<S, T, R> composer)
        {
            final FSA<S> delegated = composeDelegated(first, after, composer);
            if (delegated == null) {
                getDecoratee().compose(first, after, composer);
            }
            return delegated;
        }

        <S extends Symbol> FSA<S> determinizeDelegated(FSA<S> target);

        @Override
        default <S extends Symbol> FSA<S> determinize(FSA<S> target)
        {
            final FSA<S> delegated = determinizeDelegated(target);
            if (delegated == null) {
                getDecoratee().determinize(target);
            }
            return delegated;
        }

        <S extends Symbol> FSA<S> makeCompleteDelegated(FSA<S> target);

        @Override
        default <S extends Symbol> FSA<S> makeComplete(FSA<S> target)
        {
            final FSA<S> delegated = makeCompleteDelegated(target);
            if (delegated == null) {
                getDecoratee().makeComplete(target);
            }
            return delegated;
        }

        <S extends Symbol> FSA<S> minimizeDelegated(FSA<S> target);

        @Override
        default <S extends Symbol> FSA<S> minimize(FSA<S> target)
        {
            final FSA<S> delegated = minimizeDelegated(target);
            if (delegated == null) {
                getDecoratee().minimize(target);
            }
            return delegated;
        }

        <S extends Symbol> FSA<S> getComplementDelegated(FSA<S> target);

        @Override
        default <S extends Symbol> FSA<S> getComplement(FSA<S> target)
        {
            final FSA<S> delegated = getComplementDelegated(target);
            if (delegated == null) {
                getDecoratee().getComplement(target);
            }
            return delegated;
        }
    }
}
