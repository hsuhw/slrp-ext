package core.automata.fsa;

import api.automata.*;
import api.automata.fsa.FSA;
import core.util.Assertions;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.fsa.FSA.Builder;
import static core.util.Parameters.estimateExtendedSize;

public final class BasicFSABuilder<S> implements FSA.Builder<S>
{
    private final Alphabet.Builder<S> alphabetBuilder;
    private final DeltaFunction.Builder<S> deltaBuilder;
    private final int stateNumberEstimate;
    private final MutableSet<State> states;
    private MutableSet<State> startStates;
    private MutableSet<State> acceptStates;
    private Alphabet<S> exportingAlphabet;
    private DeltaFunction<S> exportingDelta;

    public BasicFSABuilder(int symbolNumberEstimate, S epsilonSymbol, int stateNumberEstimate)
    {
        alphabetBuilder = Alphabets.builder(symbolNumberEstimate, epsilonSymbol);
        deltaBuilder = DeltaFunctions.builder(stateNumberEstimate, symbolNumberEstimate, epsilonSymbol);
        this.stateNumberEstimate = stateNumberEstimate;
        states = UnifiedSet.newSet(stateNumberEstimate);
        startStates = UnifiedSet.newSet(stateNumberEstimate);
        acceptStates = UnifiedSet.newSet(stateNumberEstimate);
    }

    public BasicFSABuilder(MapMapDFSA<S> fsa)
    {
        alphabetBuilder = Alphabets.builderOn(fsa.getAlphabet());
        deltaBuilder = DeltaFunctions.builderBasedOn(fsa.getDeltaFunction());

        final int estimateSize = estimateExtendedSize(fsa.getStateNumber());
        this.stateNumberEstimate = estimateSize;
        states = UnifiedSet.newSet(estimateSize);
        startStates = UnifiedSet.newSet(estimateSize); // heuristic upper bound
        acceptStates = UnifiedSet.newSet(estimateSize);  // heuristic upper bound
        states.addAllIterable(fsa.getStates());
        startStates.addAllIterable(fsa.getStartStates());
        acceptStates.addAllIterable(fsa.getAcceptStates());
    }

    public BasicFSABuilder(MapMapSetNFSA<S> fsa)
    {
        alphabetBuilder = Alphabets.builderOn(fsa.getAlphabet());
        deltaBuilder = DeltaFunctions.builderBasedOn(fsa.getDeltaFunction());

        final int estimateSize = estimateExtendedSize(fsa.getStateNumber());
        this.stateNumberEstimate = estimateSize;
        states = UnifiedSet.newSet(estimateSize);
        startStates = UnifiedSet.newSet(estimateSize); // heuristic upper bound
        acceptStates = UnifiedSet.newSet(estimateSize);  // heuristic upper bound
        states.addAllIterable(fsa.getStates());
        startStates.addAllIterable(fsa.getStartStates());
        acceptStates.addAllIterable(fsa.getAcceptStates());
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
    public Builder<S> removeState(State state)
    {
        Assertions.argumentNotNull(state);

        states.remove(state);
        startStates.remove(state);
        acceptStates.remove(state);
        deltaBuilder.removeState(state);

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
    public Builder<S> addStartStates(SetIterable<State> states)
    {
        if (states.anySatisfy(Predicates.isNull())) {
            throw new IllegalArgumentException("null found in the given set");
        }

        this.states.addAllIterable(states);
        startStates.addAllIterable(states);

        return this;
    }

    @Override
    public Builder<S> resetStartStates()
    {
        startStates = UnifiedSet.newSet(stateNumberEstimate);

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
    public Builder<S> addAcceptStates(SetIterable<State> states)
    {
        if (states.anySatisfy(Predicates.isNull())) {
            throw new IllegalArgumentException("null found in the given set");
        }

        this.states.addAllIterable(states);
        acceptStates.addAllIterable(states);

        return this;
    }

    @Override
    public Builder<S> resetAcceptStates()
    {
        acceptStates = UnifiedSet.newSet(stateNumberEstimate);

        return this;
    }

    @Override
    public Builder<S> addTransition(State dept, State dest, S symbol)
    {
        addState(dept).addState(dest).addSymbol(symbol);
        deltaBuilder.addTransition(dept, dest, symbol);

        return this;
    }

    private FSA<S> settle(Alphabet<S> alphabet)
    {
        final int startStateNumber = startStates.size();
        if (startStateNumber < 1) {
            throw new IllegalStateException("no start state have been specified");
        }

        exportingAlphabet = alphabet;
        exportingDelta = deltaBuilder.build(startStateNumber != 1);
        return exportingDelta instanceof Deterministic ? new MapMapDFSA<>(this) : new MapMapSetNFSA<>(this);
    }

    @Override
    public FSA<S> build()
    {
        return settle(getCurrentAlphabet());
    }

    @Override
    public FSA<S> build(Alphabet<S> alphabet)
    {
        if (!alphabet.set().containsAllIterable(alphabetBuilder.addedSymbols())) {
            throw new IllegalArgumentException("given alphabet does not contain all the symbols");
        }

        return settle(alphabet);
    }

    Alphabet<S> getExportingAlphabet()
    {
        return exportingAlphabet;
    }

    ImmutableSet<State> getStates()
    {
        return states.toImmutable();
    }

    ImmutableSet<State> getStartStates()
    {
        return startStates.toImmutable();
    }

    ImmutableSet<State> getAcceptStates()
    {
        return acceptStates.toImmutable();
    }

    DeltaFunction<S> getExportingDelta()
    {
        return exportingDelta;
    }
}
