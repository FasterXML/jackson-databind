package com.fasterxml.jackson.databind;

import com.google.common.testing.GcFinalization;
import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

public class MapperFootprintTest {
    /*
     * Note: this class is run in an isolated execution in surefire. The test is too flaky with all the other tests
     * running in the same JVM.
     */

    @Test
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

        // 29-Nov-2022, tatu: Should be under 10k, but... flakiness.
        final int maxByteSize = 20_000;
        Assert.assertTrue(
                "ObjectMapper memory footprint ("+mapperLayout.totalSize()
                +") exceeded limit ("+maxByteSize
                +"). Footprint details: " + mapperLayout.toFootprint(),
                mapperLayout.totalSize() < maxByteSize);
    }
}
