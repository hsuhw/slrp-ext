package core.synth;

import api.synth.FSAEncoding;
import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

@RunWith(OleasterRunner.class)
public class BasicFSAEncodingTest extends AbstractFSAEncodingTest
{
    @Override
    protected FSAEncoding<Object> encodingForCommonTest()
    {
        return new BasicFSAEncoding<>(solver, 2, alphabetEncoding);
    }
}
