package core.proof;

import api.automata.AlphabetIntEncoder;
import api.proof.FSAEncoding;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class BasicFSAEncodingTest extends AbstractFSAEncodingTest
{
    @Override
    FSAEncoding<Object> newEncoding(int size, AlphabetIntEncoder<Object> alphabetEncoding)
    {
        solver.reset();

        return new BasicFSAEncoding<>(solver, size, alphabetEncoding);
    }
}
