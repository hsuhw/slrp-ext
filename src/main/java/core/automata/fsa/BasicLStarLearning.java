package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.MutableState;
import api.automata.fsa.FSA;
import api.automata.fsa.FSAs;
import api.automata.fsa.LStarLearning;
import common.util.InterruptException;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import static core.Parameters.LSTAR_LEARNING_STATE_CAPACITY;

public class BasicLStarLearning implements LStarLearning
{
    @Override
    public <S> FSA<S> learn(Alphabet<S> alphabet, Teacher<S> teacher) throws InterruptException
    {
        return new Learner<>(alphabet, teacher).learn();
    }

    private class Learner<S>
    {
        private final Alphabet<S> alphabet;
        private final Teacher<S> teacher;
        private final ListIterable<S> emptyWord;
        private DTNode bookkeepingTree;
        private MapIterable<MutableState<S>, ListIterable<S>> representativeWords;
        private MutableSet<ListIterable<S>> distinguishingWords;

        private Learner(Alphabet<S> alphabet, Teacher<S> teacher)
        {
            this.alphabet = alphabet;
            this.teacher = teacher;
            emptyWord = Lists.immutable.empty();
            distinguishingWords = Sets.mutable.empty();
        }

        private FSA<S> learn() throws InterruptException
        {
            final var trivialAnswer = attemptTrivialCase();

            return trivialAnswer != null ? trivialAnswer : compute();
        }

        private FSA<S> attemptTrivialCase() throws InterruptException
        {
            var hypothesis = FSAs.create(alphabet, LSTAR_LEARNING_STATE_CAPACITY);
            final var startBeAccept = teacher.targetAccepts(emptyWord);
            if (startBeAccept) {
                hypothesis.setAsAccept(hypothesis.startState());
            }
            final var check = teacher.checkAnswer(hypothesis);
            if (check.passed()) {
                return hypothesis;
            }

            bookkeepingTree = new DTNode(emptyWord,
                                         new DTNode(startBeAccept ? check.negativeCounterexample() : emptyWord),
                                         new DTNode(startBeAccept ? emptyWord : check.positiveCounterexample()));

            return null;
        }

        private FSA<S> compute() throws InterruptException
        {
            var hypothesis = settle();
            var check = teacher.checkAnswer(hypothesis);
            while (check.rejected()) {
                final var hasPositiveCounterexample = check.positiveCounterexample() != null;
                final var counterexample = hasPositiveCounterexample
                                           ? check.positiveCounterexample()
                                           : check.negativeCounterexample();
                var currState = hypothesis.startState();
                final MutableList<S> prefix = FastList.newList();
                DTNode lastSifted = null;
                var i = 0;
                while (true) {
                    final var sifted = bookkeepingTree.sift(prefix);
                    if (!sifted.word.equals(representativeWords.get(currState))) {

                        // have found the point where the automaton goes wrong; add a new state
                        prefix.remove(prefix.size() - 1);

                        final var distinguishingPoint = Lists.fixedSize.of(sifted); // dummy initialization
                        final var swap = new boolean[1];
                        final var representativeWord = representativeWords.get(currState);
                        bookkeepingTree.distinguish(sifted.word, representativeWord, distinguishingPoint, swap);

                        assert lastSifted != null;
                        final var nodeA = new DTNode(lastSifted.word, null, null);
                        final var nodeB = new DTNode(prefix, null, null);

                        MutableList<S> bestDistinguishingWord = FastList.newList();
                        bestDistinguishingWord.add(counterexample.get(i - 1));
                        bestDistinguishingWord.addAllIterable(distinguishingPoint.getOnly().word);

                        // check whether we can find a shorter distinguishing word
                        for (var previousFinding : distinguishingWords) {
                            final MutableList<S> previousFindingPrefix = FastList.newList();
                            for (var j = 0; j < previousFinding.size() && j < bestDistinguishingWord.size() - 1; j++) {
                                previousFindingPrefix.add(previousFinding.get(j));
                                if (previousFindingPrefix.equals(bestDistinguishingWord)) {
                                    continue;
                                }

                                final var a = Lists.mutable.ofAll(nodeA.word);
                                final var b = Lists.mutable.ofAll(nodeB.word);
                                a.addAll(previousFindingPrefix);
                                b.addAll(previousFindingPrefix);

                                if (teacher.targetAccepts(a) != teacher.targetAccepts(b)) {
                                    bestDistinguishingWord = previousFindingPrefix;
                                }
                            }
                        }

                        distinguishingWords.add(bestDistinguishingWord);
                        lastSifted.word = bestDistinguishingWord;

                        final var a = Lists.mutable.ofAll(nodeA.word);
                        a.addAll(bestDistinguishingWord);
                        if (teacher.targetAccepts(a)) {
                            lastSifted.right = nodeA;
                            lastSifted.left = nodeB;
                        } else {
                            lastSifted.right = nodeB;
                            lastSifted.left = nodeA;
                        }

                        break;
                    }

                    lastSifted = sifted;

                    final var nextChar = counterexample.get(i++);
                    prefix.add(nextChar);
                    currState = currState.successor(nextChar);
                }

                hypothesis = settle();
                if (hasPositiveCounterexample == hypothesis.accepts(counterexample)) {
                    check = teacher.checkAnswer(hypothesis);
                }
            }

            return hypothesis;
        }

        private FSA<S> settle()
        {
            final List<ListIterable<S>> leafWords = new LinkedList<>();
            bookkeepingTree.collectLeafWords(leafWords);

            final var capacity = leafWords.size();
            final var result = FSAs.create(alphabet, capacity);
            final MutableMap<MutableState<S>, ListIterable<S>> stateToWord = UnifiedMap.newMap(capacity);
            final MutableMap<ListIterable<S>, MutableState<S>> wordToState = UnifiedMap.newMap(capacity);
            for (var word : leafWords) {
                final var state = result.newState();
                stateToWord.put(state, word);
                wordToState.put(word, state);
            }
            final var dummyStart = result.startState();
            result.setAsStart(wordToState.get(emptyWord));
            result.removeState(dummyStart);

            for (var i = 0; i < 2; i++) {
                final var it = (i == 0 ? bookkeepingTree.left : bookkeepingTree.right).leafIterator();
                while (it.hasNext()) {
                    final var leaf = it.next();
                    final var state = wordToState.get(leaf.word);
                    final MutableList<S> word = FastList.newList(leaf.word.size() + 1);
                    word.addAllIterable(leaf.word);

                    for (var symbol : alphabet.noEpsilonSet()) {
                        word.add(symbol);
                        result.addTransition(state, wordToState.get(bookkeepingTree.sift(word).word), symbol);
                        word.remove(word.size() - 1);
                    }

                    if (i == 1) {
                        result.setAsAccept(state);
                    }
                }
            }

            representativeWords = stateToWord;
            return result;
        }

        private class DTNode
        {
            private ListIterable<S> word;
            private DTNode left;
            private DTNode right;

            /**
             * The children should be either both null or both non-null.
             */
            private DTNode(ListIterable<S> word, DTNode left, DTNode right)
            {
                this.word = word;
                this.left = left;
                this.right = right;
            }

            private DTNode(ListIterable<S> word)
            {
                this(word, null, null);
            }

            private DTNode sift(MutableList<S> word)
            {
                if (left == null && right == null) {
                    return this;
                }

                final var originSize = word.size();
                word.addAllIterable(this.word);
                final var targetAccepts = teacher.targetAccepts(word);
                while (word.size() > originSize) {
                    word.remove(word.size() - 1);
                }

                return (targetAccepts ? right : left).sift(word);
            }

            private void collectLeafWords(List<ListIterable<S>> result)
            {
                if (left == null || right == null) {
                    result.add(word);
                    return;
                }

                left.collectLeafWords(result);
                right.collectLeafWords(result);
            }

            private int distinguish(ListIterable<S> word1, ListIterable<S> word2, List<DTNode> result, boolean[] swap)
            {
                if (left == null || right == null) {
                    if (word.equals(word1)) {
                        return 1;
                    } else if (word.equals(word2)) {
                        return 2;
                    } else {
                        return 0;
                    }
                }

                final var leftReturnCode = left.distinguish(word1, word2, result, swap);
                if (leftReturnCode == 3) { // found
                    return 3;
                }

                final var rightReturnCode = right.distinguish(word1, word2, result, swap);
                if (leftReturnCode == 1 && rightReturnCode == 2) {
                    result.set(0, this);
                    swap[0] = false;
                    return 3;
                }
                if (leftReturnCode == 2 && rightReturnCode == 1) {
                    result.set(0, this);
                    swap[0] = true;
                    return 3;
                }

                assert (leftReturnCode == 0 || rightReturnCode == 0);
                return leftReturnCode + rightReturnCode;
            }

            private Iterator<DTNode> leafIterator()
            {
                final Stack<DTNode> stack = new Stack<>();
                stack.push(this);

                return new Iterator<>()
                {
                    private final Stack<DTNode> pendingNodes = stack;

                    public boolean hasNext()
                    {
                        return !pendingNodes.empty();
                    }

                    public DTNode next()
                    {
                        var currNode = pendingNodes.pop();

                        while (currNode.left != null) {
                            pendingNodes.push(currNode.right);
                            currNode = currNode.left; // to the deepest left
                        }

                        return currNode;
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }
    }
}
