package core.automata;

import com.mscharhag.oleaster.runner.OleasterRunner;
import org.junit.runner.RunWith;

import static api.automata.TransitionGraph.Builder;
import static com.mscharhag.oleaster.matcher.Matchers.expect;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.describe;
import static com.mscharhag.oleaster.runner.StaticRunnerSupport.it;

@RunWith(OleasterRunner.class)
public class MapMapSetGraphTest extends AbstractTransitionGraphTest
{
    @Override
    protected Builder<Object, Object> builderForCommonTest()
    {
        final int c = 3;

        return new MapMapSetGraphBuilder<>(c, c, epsilon);
    }

    {
        describe("When its builder being create", () -> {

            it("ensures taking a non-null epsilon label", () -> {
                final int c = 1;

                expect(() -> new MapMapSetGraphBuilder<>(c, c, nullArg)).toThrow(IllegalArgumentException.class);
            });

            it("accepts no negative capacity", () -> {
                final int c = 1;
                final int neg = -1;

                expect(() -> new MapMapSetGraphBuilder<>(neg, c, epsilon)).toThrow(IllegalArgumentException.class);
                expect(() -> new MapMapSetGraphBuilder<>(c, neg, epsilon)).toThrow(IllegalArgumentException.class);
            });

        });
    }
}
