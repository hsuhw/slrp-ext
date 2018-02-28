package api.util;

import api.automata.State;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;

import static api.automata.Automaton.StepMaker;

public interface Connectives
{
    BooleanBooleanPredicate AND = (a, b) -> a && b;
    BooleanBooleanPredicate OR = (a, b) -> a || b;

    interface Labels
    {
        static <S extends State<T>, T> StepMaker<S, T, S, T, T> matched()
        {
            return (statePair, s1, s2) -> s1.equals(s2) ? s1 : null;
        }

        static <S, T, U extends Pair<S, T>> SymbolDecider<U, S, U> whoseInputMatched()
        {
            return (trans, symbol) -> symbol.equals(trans.getOne()) ? trans : null;
        }

        static <S, T, U extends Pair<S, T>> SymbolDecider<U, S, U> whoseOutputMatched()
        {
            return (trans, symbol) -> symbol.equals(trans.getTwo()) ? trans : null;
        }

        static <S, T, U extends Pair<S, T>> SymbolDecider<U, S, T> transducible()
        {
            return (trans, symbol) -> symbol.equals(trans.getOne()) ? trans.getTwo() : null;
        }

        static <S, T, X extends Pair<S, T>, Y extends Pair<T, S>> SymbolDecider<X, Y, Twin<S>> composable()
        {
            return (s1, s2) -> s1.getTwo().equals(s2.getOne()) ? Tuples.twin(s1.getOne(), s2.getTwo()) : null;
        }
    }
}
