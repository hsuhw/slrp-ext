package synth;

import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class Sat4jSolverAdapterTest extends SatSolverTest
{
    {
        solver = new Sat4jSolverAdapter();
    }
}
