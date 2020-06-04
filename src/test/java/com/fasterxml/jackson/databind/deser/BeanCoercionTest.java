package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public class BeanCoercionTest extends BaseMapTest
{
    static class Bean {
        public String a;
    }

    /*
    /********************************************************
    /* Test methods
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testPOJOFromEmptyString() throws Exception
    {
        // first, verify default settings which do not accept empty String:
        assertFalse(MAPPER.isEnabled(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT));
        try {
            MAPPER.readValue(quote(""), Bean.class);
            fail("Should not accept Empty String for POJO");
        } catch (JsonProcessingException e) {
            verifyException(e, "Cannot deserialize value of type ");
            verifyException(e, " from empty String ");
            assertValidLocation(e.getLocation());
        }
        // should be ok to enable dynamically
        ObjectReader r = MAPPER.readerFor(Bean.class)
                .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        Bean result = r.readValue(quote(""));
        assertNull(result);
    }
}
