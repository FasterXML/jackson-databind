package com.fasterxml.jackson.databind.seq;

import com.fasterxml.jackson.databind.*;

/**
 * Tests to verify aspects of error recover for reading using
 * iterator.
 */
public class ReadRecoveryTest extends BaseMapTest
{
    static class Bean {
        public int a;

        @Override public String toString() { return "{Bean, a="+a+"}"; }
    }

    /*
    /**********************************************************
    /* Unit tests; root-level value sequences via Mapper
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testRootBeans() throws Exception
    {
        final String JSON = aposToQuotes("{'a':3} {'b':5}");
        MappingIterator<Bean> it = MAPPER.reader(Bean.class).readValues(JSON);
        // First one should be fine
        assertTrue(it.hasNextValue());
        Bean bean = it.nextValue();
        assertEquals(3, bean.a);
        // but second one not
        try {
            bean = it.nextValue();
            fail("Should not have succeeded");
        } catch (JsonMappingException e) {
            verifyException(e, "Unrecognized field");
        }
        // 24-Mar-2015, tatu: With 2.5, best we can do is to avoid infinite loop;
        //    also, since the next token is END_OBJECT, will produce empty Object
        assertTrue(it.hasNextValue());
        bean = it.nextValue();
        assertEquals(0, bean.a);
        // and we should be done now
        assertFalse(it.hasNextValue());
        it.close();
    }
}
