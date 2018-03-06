package core.automata;

import api.automata.MutableState;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;


@RunWith(OleasterRunner.class)
public class MapSetStateTest extends AbstractMutableStateTest
{
    @Override
    protected MutableState<Object> newState()
    {
        return new MapSetState<>(3);
    }
}
