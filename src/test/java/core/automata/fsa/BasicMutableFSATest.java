package core.automata.fsa;

import api.automata.Alphabet;
import api.automata.fsa.MutableFSA;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class BasicMutableFSATest extends AbstractMutableFSATest
{
    @Override
    MutableFSA<Object> newFSA(Alphabet<Object> alphabet, int stateCapacity)
    {
        return new BasicMutableFSA<>(alphabet, stateCapacity);
    }
}
