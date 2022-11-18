package com.fasterxml.jackson.databind;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

public class MapperFootprintTest {
    @Test
    @Ignore
    public void testMapperFootprint() {
        // memory footprint limit for the ObjectMapper
        GraphLayout mapperLayout = GraphLayout.parseInstance(new ObjectMapper())
                .subtract(GraphLayout.parseInstance(new ObjectMapper()));
        System.out.println(mapperLayout.toFootprint());
        Assert.assertTrue(mapperLayout.totalSize() < 50000);
    }
}
