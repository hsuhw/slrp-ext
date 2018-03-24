package core.automata.fst;

import api.automata.Alphabet;
import api.automata.fst.MutableFST;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.eclipse.collections.api.tuple.Pair;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class BasicMutableFSTTest extends AbstractMutableFSTTest
{
    @Override
    protected <S, T> MutableFST<S, T> newFST(Alphabet<Pair<S, T>> alphabet, int stateCapacity)
    {
        return new BasicMutableFST<>(alphabet, stateCapacity);
    }
}
