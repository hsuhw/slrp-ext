package core.automata.fsa;

import api.automata.fsa.LStarLearning;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class BasicLStarLearningTest extends AbstractLStarLearningTest
{
    @Override
    LStarLearning getAlgorithm()
    {
        return new BasicLStarLearning();
    }
}
