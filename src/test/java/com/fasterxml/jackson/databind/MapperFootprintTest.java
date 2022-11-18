package com.fasterxml.jackson.databind;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

public class MapperFootprintTest {
    @Test
    public void testMapperFootprint() {
        // memory footprint limit for the ObjectMapper
        GraphLayout mapperLayout = GraphLayout.parseInstance(new ObjectMapper());
        Assert.assertTrue(mapperLayout.totalSize() < 50000);
        System.out.println(mapperLayout.toFootprint());
    }
}
