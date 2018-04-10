package api.automata.fsa;

import api.automata.Alphabet;
import api.automata.AlphabetEncoder;
import api.automata.AlphabetEncoders;
import api.automata.MutableState;
import common.VATACommands;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.bimap.mutable.HashBiMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.lang.ref.SoftReference;
import java.util.WeakHashMap;
import java.util.function.Function;

import static common.util.Constants.DISPLAY_NEWLINE;

public final class VATA
{
    // TODO: verify that the caching is working correctly
    private static final WeakHashMap<Alphabet, SoftReference<AlphabetEncoder>> ALPHABET_ENCODER_CACHE;

    static {
        ALPHABET_ENCODER_CACHE = new WeakHashMap<>();
    }

    private VATA()
    {
    }

    private static <S> AlphabetEncoder<S, String> getAlphabetEncoder(Alphabet<S> alphabet)
    {
        final var cache = ALPHABET_ENCODER_CACHE.get(alphabet);
        AlphabetEncoder cachedItem;
        if (cache != null && (cachedItem = cache.get()) != null) {
            @SuppressWarnings("unchecked")
            final AlphabetEncoder<S, String> result = cachedItem;
            return result;
        }

        final MutableBiMap<S, String> definition = new HashBiMap<>(alphabet.size());
        definition.put(alphabet.epsilon(), "a0");
        var i = 1;
        for (var symbol : alphabet.noEpsilonSet()) {
            definition.put(symbol, "a" + i++);
        }
        final var result = AlphabetEncoders.create(definition, alphabet.epsilon());
        ALPHABET_ENCODER_CACHE.put(alphabet, new SoftReference<>(result));

        return result;
    }

    private static <S> String toTimbukFormat(FSA<S> target)
    {
        final var result = new StringBuilder();
        final var alphabet = target.alphabet();
        final var alphabetEncoder = getAlphabetEncoder(alphabet);

        result.append("Ops init:0");
        alphabet.noEpsilonSet().forEach(s -> result.append(" ").append(alphabetEncoder.encode(s)).append(":1"));
        result.append(DISPLAY_NEWLINE).append(DISPLAY_NEWLINE);

        result.append("Automaton A").append(DISPLAY_NEWLINE);

        result.append("States");
        final var stateNames = target.stateNames();
        target.states().forEach(state -> result.append(" ").append(stateNames.get(state)));
        result.append(DISPLAY_NEWLINE);

        result.append("Final States");
        target.acceptStates().forEach(state -> result.append(" ").append(stateNames.get(state)));
        result.append(DISPLAY_NEWLINE);

        result.append("Transitions").append(DISPLAY_NEWLINE);
        result.append("init -> ").append(stateNames.get(target.startState())).append(DISPLAY_NEWLINE);
        target.states().forEach(state -> state.enabledSymbols().forEach(symbol -> {
            state.successors(symbol).forEach(destination -> {
                final var s = alphabetEncoder.encode(symbol);
                final var dept = stateNames.get(state);
                final var dest = stateNames.get(destination);
                result.append(s).append("(").append(dept).append(") -> ").append(dest).append(DISPLAY_NEWLINE);
            });
        }));

        return result.toString();
    }

    public static <S> FSA<S> reduce(FSA<S> target)
    {
        final var resultInTimbukFormat = VATACommands.reduce(toTimbukFormat(target));
        final var resultTokens = resultInTimbukFormat.split("(\\s|\\(|\\)|->)+");
        if (!resultTokens[5].equals("States")) {
            throw new IllegalStateException("unexpected result received");
        }
        if (resultTokens[6].equals("Transitions")) {
            return FSAs.acceptingNone(target.alphabet());
        }

        final var capacity = target.states().size();
        final var result = FSAs.create(target.alphabet(), capacity); // upper bound
        final MutableMap<String, MutableState<S>> stateNameTable = UnifiedMap.newMap(capacity);
        final Function<String, MutableState<S>> takeState = name -> //
            stateNameTable.computeIfAbsent(name, __ -> result.newState());

        var index = 6;
        String acceptStateName;
        while (!(acceptStateName = resultTokens[index++]).equals("Transitions")) {
            result.setAsAccept(takeState.apply(acceptStateName));
        }
        if (!resultTokens[index++].equals("init")) {
            throw new IllegalStateException("unexpected result received");
        }
        final var dummyStart = result.startState();
        result.setAsStart(takeState.apply(resultTokens[index++]));
        result.removeState(dummyStart);

        final var alphabetEncoder = getAlphabetEncoder(target.alphabet());
        while (index != resultTokens.length) {
            final var symbol = alphabetEncoder.decode(resultTokens[index++]);
            final var dept = takeState.apply(resultTokens[index++]);
            final var dest = takeState.apply(resultTokens[index++]);
            result.addTransition(dept, dest, symbol);
        }

        return result;
    }

    public static <S> boolean checkInclusion(FSA<S> subsumer, FSA<S> includer)
    {
        return VATACommands.checkInclusion(toTimbukFormat(subsumer), toTimbukFormat(includer));
    }
}
