package com.fasterxml.jackson.databind.interop;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public class TestFormatDetection extends BaseMapTest
{
    private final ObjectReader READER = objectReader();

    private final JsonFactory JSON_FACTORY = new JsonFactory();

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
        ObjectReader detecting = READER.withType(POJO.class).withFormatDetection(JSON_FACTORY);
        POJO pojo = detecting.readValue(utf8Bytes("{\"x\":1}"));
        assertNotNull(pojo);
        assertEquals(1, pojo.x);
    }

    public void testInvalid() throws Exception
    {
        ObjectReader detecting = READER.withType(POJO.class).withFormatDetection(JSON_FACTORY);
        try {
            detecting.readValue(utf8Bytes("<POJO><x>1</x></POJO>"));
            fail("Should have failed");
        } catch (JsonProcessingException e) {
            verifyException(e, "Can not detect format from input");
        }
    }
}
