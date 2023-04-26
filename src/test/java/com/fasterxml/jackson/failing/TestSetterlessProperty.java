package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestSetterlessProperty
    extends BaseMapTest
{
    static class ImmutableId {
        private final int id;

        public ImmutableId(int id) { this.id = id; }

        public int getId() {
            return id;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // For [databind#501]
    public void testSetterlessProperty() throws Exception
    {
        ImmutableId input = new ImmutableId(13);
        ObjectMapper m = new ObjectMapper();
        String json = m.writerWithDefaultPrettyPrinter().writeValueAsString(input);

        ImmutableId output = m.readValue(json, ImmutableId.class);
        assertNotNull(output);

        assertEquals(input.id, output.id);
    }
}
