package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import core.util.Assertions;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Set;

import static api.automata.fsa.FSA.Builder;

public class BasicFSABuilder<S> implements Builder<S>
{
    private final Alphabet.Builder<S> alphabetBuilder;
    private final DeltaFunction.Builder<S> deltaBuilder;
    private final MutableSet<State> states;
    private final MutableSet<State> startStates;
    private final MutableSet<State> acceptStates;

    public BasicFSABuilder(int symbolNumberEstimate, S epsilonSymbol, int stateNumberEstimate)
    {
        alphabetBuilder = Alphabets.builder(symbolNumberEstimate);
        alphabetBuilder.defineEpsilon(epsilonSymbol);
        deltaBuilder = DeltaFunctions.builder(stateNumberEstimate, epsilonSymbol);
        states = UnifiedSet.newSet(stateNumberEstimate);
        startStates = UnifiedSet.newSet(stateNumberEstimate);
        acceptStates = UnifiedSet.newSet(stateNumberEstimate);
    }

    @Override
    public Alphabet<S> getCurrentAlphabet()
    {
        return alphabetBuilder.build();
    }

    @Override
    public Builder<S> addSymbol(S symbol)
    {
        Assertions.argumentNotNull(symbol);

        alphabetBuilder.add(symbol);

        return this;
    }

    @Override
    public Builder<S> addState(State state)
    {
        Assertions.argumentNotNull(state);

        states.add(state);

        return this;
    }

    @Override
    public Builder<S> addStartState(State state)
    {
        Assertions.argumentNotNull(state);

        addState(state);
        startStates.add(state);

        return this;
    }

    @Override
    public Builder<S> addAcceptState(State state)
    {
        Assertions.argumentNotNull(state);

        addState(state);
        acceptStates.add(state);

        return this;
    }

    @Override
    public Builder<S> addTransition(State dept, State dest, S symbol)
    {
        addState(dept).addState(dest).addSymbol(symbol);
        deltaBuilder.addTransition(dept, dest, symbol);

        return this;
    }

    public static <S> FSA<S> make(Alphabet<S> alphabet, MutableSet<State> states, MutableSet<State> startStates,
                                  MutableSet<State> acceptStates, DeltaFunction.Builder<S> deltaBuilder)
    {
        final int startStateNumber = startStates.size();
        if (startStateNumber < 1) {
            throw new IllegalStateException("no start states specified");
        }

        final DeltaFunction<S> delta = deltaBuilder.build(startStateNumber != 1);
        if (delta instanceof Deterministic) {
            return new MapMapDFSA<>(alphabet, states.toImmutable(), startStates.toImmutable(),
                                    acceptStates.toImmutable(), delta);
        } else {
            return new MapMapSetNFSA<>(alphabet, states.toImmutable(), startStates.toImmutable(),
                                       acceptStates.toImmutable(), delta);
        }
    }

    private FSA<S> settle(Alphabet<S> alphabet)
    {
        return make(alphabet, states, startStates, acceptStates, deltaBuilder);
    }

    @Override
    public FSA<S> build()
    {
        return settle(getCurrentAlphabet());
    }

    @Override
    public FSA<S> build(Alphabet<S> alphabet)
    {
        if (!alphabet.getSet().containsAll((Set) alphabetBuilder.getAddedSymbols())) {
            throw new IllegalArgumentException("given alphabet does not contain all the symbols");
        }

        return settle(alphabet);
    }
}
