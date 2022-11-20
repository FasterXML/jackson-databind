package tools.jackson.databind;

import com.google.common.testing.GcFinalization;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

public class MapperFootprintTest {
    @Test
    @Ignore
    public void testMapperFootprint() throws InterruptedException {
        // memory footprint limit for the ObjectMapper

        // force gc (see javadoc of GraphLayout.subtract)
        GcFinalization.awaitFullGc();
        // do this calculation twice. If there's a GC in one case, and the subtract call doesn't work well because of
        // this, we can fall back to the other.
        GraphLayout mapperLayoutA = GraphLayout.parseInstance(new ObjectMapper())
                .subtract(GraphLayout.parseInstance(new ObjectMapper()));
        GraphLayout mapperLayoutB = GraphLayout.parseInstance(new ObjectMapper())
                .subtract(GraphLayout.parseInstance(new ObjectMapper()));
        GraphLayout mapperLayout = mapperLayoutA.totalSize() > mapperLayoutB.totalSize() ?
                mapperLayoutB : mapperLayoutA;

        Assert.assertTrue(
                "ObjectMapper memory footprint exceeded limit. Footprint details: " + mapperLayout.toFootprint(),
                mapperLayout.totalSize() < 10000);
    }
}
