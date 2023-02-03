package com.fasterxml.jackson.databind.interop;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.*;

public class TestFormatDetection extends BaseMapTest
{
    private final ObjectReader READER = objectReader();

    static class POJO {
        public int x, y;

        public POJO() { }
        public POJO(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testSimpleWithJSON() throws Exception
    {
        ObjectReader detecting = READER.forType(POJO.class);
        detecting = detecting.withFormatDetection(detecting);
        POJO pojo = detecting.readValue(utf8Bytes("{\"x\":1}"));
        assertNotNull(pojo);
        assertEquals(1, pojo.x);
    }

    public void testSequenceWithJSON() throws Exception
    {
        ObjectReader detecting = READER.forType(POJO.class);
        detecting = detecting.withFormatDetection(detecting);
        MappingIterator<POJO> it = detecting.
                readValues(utf8Bytes(a2q("{'x':1}\n{'x':2,'y':5}")));

        assertTrue(it.hasNextValue());
        POJO pojo = it.nextValue();
        assertEquals(1, pojo.x);

        assertTrue(it.hasNextValue());
        pojo = it.nextValue();
        assertEquals(2, pojo.x);
        assertEquals(5, pojo.y);

        assertFalse(it.hasNextValue());
        it.close();

        // And again with nodes
        ObjectReader r2 = READER.forType(JsonNode.class);
        r2 = r2.withFormatDetection(r2);
        MappingIterator<JsonNode> nodes = r2.
                readValues(utf8Bytes(a2q("{'x':1}\n{'x':2,'y':5}")));

        assertTrue(nodes.hasNextValue());
        JsonNode n = nodes.nextValue();
        assertEquals(1, n.size());

        assertTrue(nodes.hasNextValue());
        n = nodes.nextValue();
        assertEquals(2, n.size());
        assertEquals(2, n.path("x").asInt());
        assertEquals(5, n.path("y").asInt());

        assertFalse(nodes.hasNextValue());
        nodes.close();
    }

    public void testInvalid() throws Exception
    {
        ObjectReader detecting = READER.forType(POJO.class);
        detecting = detecting.withFormatDetection(detecting);
        try {
            detecting.readValue(utf8Bytes("<POJO><x>1</x></POJO>"));
            fail("Should have failed");
        } catch (StreamReadException e) {
            verifyException(e, "Cannot detect format from input");
        }
    }
}
