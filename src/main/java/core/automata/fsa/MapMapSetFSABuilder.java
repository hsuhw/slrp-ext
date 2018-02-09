package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.Alphabets;
import api.automata.State;
import api.automata.TransitionGraph;
import api.automata.fsa.FSA;
import common.util.Assert;
import core.automata.MapMapSetGraph;
import core.automata.MapMapSetGraphBuilder;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static api.automata.fsa.FSA.Builder;
import static core.Parameters.estimateExtendedSize;

public class MapMapSetFSABuilder<S> implements FSA.Builder<S>
{
    private final Alphabet.Builder<S> alphabetBuilder;
    private final MapMapSetGraphBuilder<State, S> deltaBuilder;
    private final int stateCapacity;
    private final MutableSet<State> states;
    private MutableSet<State> startStates;
    private MutableSet<State> acceptStates;
    private Alphabet<S> exportingAlphabet;

    public MapMapSetFSABuilder(int stateCapacity, int symbolCapacity, S epsilonSymbol)
    {
        alphabetBuilder = Alphabets.builder(symbolCapacity, epsilonSymbol);
        deltaBuilder = new MapMapSetGraphBuilder<>(stateCapacity, symbolCapacity, epsilonSymbol);
        this.stateCapacity = stateCapacity;
        states = UnifiedSet.newSet(stateCapacity);
        startStates = UnifiedSet.newSet(stateCapacity);
        acceptStates = UnifiedSet.newSet(stateCapacity);
    }

    public MapMapSetFSABuilder(MapMapSetFSA<S> fsa, int stateCapacity, int transitionCapacity)
    {
        alphabetBuilder = Alphabets.builder(fsa.alphabet());
        deltaBuilder = new MapMapSetGraphBuilder<>(fsa.transitionGraph(), stateCapacity, transitionCapacity);

        this.stateCapacity = stateCapacity;
        states = UnifiedSet.newSet(stateCapacity);
        startStates = UnifiedSet.newSet(stateCapacity);
        acceptStates = UnifiedSet.newSet(stateCapacity);
        states.addAllIterable(fsa.states());
        startStates.addAllIterable(fsa.startStates());
        acceptStates.addAllIterable(fsa.acceptStates());
    }

    public MapMapSetFSABuilder(MapMapSetFSA<S> fsa)
    {
        alphabetBuilder = Alphabets.builder(fsa.alphabet());
        deltaBuilder = new MapMapSetGraphBuilder<>(fsa.transitionGraph());

        final int stateNumberEstimate = estimateExtendedSize(fsa.states().size());
        stateCapacity = stateNumberEstimate;
        states = UnifiedSet.newSet(stateNumberEstimate);
        startStates = UnifiedSet.newSet(stateNumberEstimate);
        acceptStates = UnifiedSet.newSet(stateNumberEstimate);
        states.addAllIterable(fsa.states());
        startStates.addAllIterable(fsa.startStates());
        acceptStates.addAllIterable(fsa.acceptStates());
    }

    @Override
    public Alphabet<S> currentAlphabet()
    {
        return alphabetBuilder.build();
    }

    @Override
    public int currentStateNumber()
    {
        return states.size();
    }

    @Override
    public int currentStartStateNumber()
    {
        return startStates.size();
    }

    @Override
    public int currentAcceptStateNumber()
    {
        return acceptStates.size();
    }

    @Override
    public int currentTransitionNumber()
    {
        return deltaBuilder.currentSize();
    }

    @Override
    public Builder<S> addSymbol(S symbol)
    {
        Assert.argumentNotNull(symbol);

        alphabetBuilder.add(symbol);

        return this;
    }

    @Override
    public Builder<S> addState(State state)
    {
        Assert.argumentNotNull(state);

        states.add(state);

        return this;
    }

    @Override
    public Builder<S> removeState(State state)
    {
        Assert.argumentNotNull(state);

        states.remove(state);
        startStates.remove(state);
        acceptStates.remove(state);
        deltaBuilder.removeNode(state);

        return this;
    }

    @Override
    public Builder<S> addStartState(State state)
    {
        Assert.argumentNotNull(state);

        addState(state);
        startStates.add(state);

        return this;
    }

    @Override
    public Builder<S> addStartStates(ImmutableSet<State> states)
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
        startStates = UnifiedSet.newSet(stateCapacity);

        return this;
    }

    @Override
    public Builder<S> addAcceptState(State state)
    {
        Assert.argumentNotNull(state);

        addState(state);
        acceptStates.add(state);

        return this;
    }

    @Override
    public Builder<S> addAcceptStates(ImmutableSet<State> states)
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
        acceptStates = UnifiedSet.newSet(stateCapacity);

        return this;
    }

    @Override
    public Builder<S> addTransition(State dept, State dest, S symbol)
    {
        addState(dept).addState(dest).addSymbol(symbol);
        deltaBuilder.addArc(dept, dest, symbol);

        return this;
    }

    @Override
    public Builder<S> addEpsilonTransition(State dept, State dest)
    {
        addState(dept).addState(dest);
        deltaBuilder.addEpsilonArc(dept, dest);

        return null;
    }

    @Override
    public Builder<S> addTransitions(TransitionGraph<State, S> graph)
    {
        ((MapMapSetGraph<State, S>) graph).forwardGraph().forEachKeyValue((dept, arcRecord) -> {
            arcRecord.forEachKeyValue((arcLabel, dests) -> {
                dests.forEach(dest -> addTransition(dept, dest, arcLabel));
            });
        });

        return this;
    }

    private FSA<S> settle(Alphabet<S> alphabet)
    {
        if (startStates.size() < 1) {
            throw new IllegalStateException("no start state have been specified");
        }

        exportingAlphabet = alphabet;

        return new MapMapSetFSA<>(this);
    }

    @Override
    public FSA<S> build()
    {
        return settle(currentAlphabet());
    }

    @Override
    public FSA<S> buildWith(Alphabet<S> override)
    {
        if (!override.asSet().containsAllIterable(alphabetBuilder.addedSymbols())) {
            throw new IllegalArgumentException("given alphabet does not contain all the symbols");
        }

        return settle(override);
    }

    final Alphabet<S> exportAlphabet()
    {
        return exportingAlphabet;
    }

    final ImmutableSet<State> states()
    {
        return states.toImmutable();
    }

    final ImmutableSet<State> startStates()
    {
        return startStates.toImmutable();
    }

    final ImmutableSet<State> acceptStates()
    {
        return acceptStates.toImmutable();
    }

    final MapMapSetGraph<State, S> exportDelta()
    {
        return deltaBuilder.build();
    }
}
