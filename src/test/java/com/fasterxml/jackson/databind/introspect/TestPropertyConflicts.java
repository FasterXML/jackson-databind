package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests verifying handling of potential and actual
 * conflicts, regarding property handling.
 */
public class TestPropertyConflicts extends BaseMapTest
{
    // For [JACKSON-694]: error message for conflicting getters sub-optimal
    static class BeanWithConflict
    {
        public int getX() { return 3; }
        public boolean getx() { return false; }
    }

    // [Issue#238]
    protected static class Getters1A
    {
        protected int value = 3;
        
        public int getValue() { return value+1; }
        public boolean isValue() { return false; }
    }

    // variant where order of declarations is reversed; to try to
    // ensure declaration order won't break things
    protected static class Getters1B
    {
        public boolean isValue() { return false; }

        protected int value = 3;
        
        public int getValue() { return value+1; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = objectMapper();
    
    // for [JACKSON-694]
    public void testFailWithDupProps() throws Exception
    {
        BeanWithConflict bean = new BeanWithConflict();
        try {
            String json = MAPPER.writeValueAsString(bean);
            fail("Should have failed due to conflicting accessor definitions; got JSON = "+json);
        } catch (JsonProcessingException e) {
            verifyException(e, "Conflicting getter definitions");
        }
    }        

    // [Issue#238]: ok to have getter, "isGetter"
    public void testRegularAndIsGetter() throws Exception
    {
        // first, serialize without probs:
        assertEquals("{\"value\":4}", MAPPER.writeValueAsString(new Getters1A()));
        assertEquals("{\"value\":4}", MAPPER.writeValueAsString(new Getters1B()));

        // and similarly, deserialize
        assertEquals(1, MAPPER.readValue("{\"value\":1}", Getters1A.class).value);
        assertEquals(2, MAPPER.readValue("{\"value\":2}", Getters1B.class).value);
    }
}
