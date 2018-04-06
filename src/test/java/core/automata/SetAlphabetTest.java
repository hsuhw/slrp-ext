package core.automata;

import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

import static api.automata.Alphabet.Builder;

@RunWith(OleasterRunner.class)
public class SetAlphabetTest extends AbstractAlphabetTest
{
    @Override
    Builder<Object> newBuilder(int capacity, Object epsilon)
    {
        return new SetAlphabetBuilder<>(capacity, epsilon);
    }
}
