package com.fasterxml.jackson.databind.interop;

import com.fasterxml.jackson.core.JsonProcessingException;

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
        ObjectReader detecting = READER.withType(POJO.class);
        detecting = detecting.withFormatDetection(detecting);
        POJO pojo = detecting.readValue(utf8Bytes("{\"x\":1}"));
        assertNotNull(pojo);
        assertEquals(1, pojo.x);
    }

    public void testInvalid() throws Exception
    {
        ObjectReader detecting = READER.withType(POJO.class);
        detecting = detecting.withFormatDetection(detecting);
        try {
            detecting.readValue(utf8Bytes("<POJO><x>1</x></POJO>"));
            fail("Should have failed");
        } catch (JsonProcessingException e) {
            verifyException(e, "Can not detect format from input");
        }
    }
}
