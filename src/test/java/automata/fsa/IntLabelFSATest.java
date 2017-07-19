package automata.fsa;

import com.mscharhag.oleaster.runner.OleasterRunner;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.junit.runner.RunWith;
import parser.fsa.IntLabelFSABasicParser;

import java.io.InputStream;
import java.util.List;

import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

@RunWith(OleasterRunner.class)
public class IntLabelFSATest
{{
    describe("Parser", () -> {

        it("gives a quick overview", () -> {
            final InputStream is = this.getClass().getResourceAsStream("fsa.source");
            final IntLabelFSABasicParser parser = new IntLabelFSABasicParser(new ObjectIntHashMap<>());
            List<IntLabelFSA> automatonList = parser.parse(is);
            System.out.println(automatonList.get(0));
            System.out.println(automatonList.get(1));
        });

    });
}}
