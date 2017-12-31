package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.Optional;

import com.fasterxml.jackson.databind.*;

public class OptionalBooleanTest extends BaseMapTest
{
    static class BooleanBean {
        public Optional<Boolean> value;

        public BooleanBean() { }
        public BooleanBean(Boolean b) {
            value = Optional.ofNullable(b);
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    // for [datatype-jdk8#23]
    public void testBoolean() throws Exception
    {
        // First, serialization
        String json = MAPPER.writeValueAsString(new BooleanBean(true));
        assertEquals(aposToQuotes("{'value':true}"), json);
        json = MAPPER.writeValueAsString(new BooleanBean());
        assertEquals(aposToQuotes("{'value':null}"), json);
        json = MAPPER.writeValueAsString(new BooleanBean(null));
        assertEquals(aposToQuotes("{'value':null}"), json);

        // then deser
        BooleanBean b = MAPPER.readValue(aposToQuotes("{'value':null}"), BooleanBean.class);
        assertNotNull(b.value);
        assertFalse(b.value.isPresent());

        b = MAPPER.readValue(aposToQuotes("{'value':false}"), BooleanBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertFalse(b.value.get().booleanValue());

        b = MAPPER.readValue(aposToQuotes("{'value':true}"), BooleanBean.class);
        assertNotNull(b.value);
        assertTrue(b.value.isPresent());
        assertTrue(b.value.get().booleanValue());

        // and looks like a special, somewhat non-conforming case is what a user had
        // issues with
        b = MAPPER.readValue(aposToQuotes("{'value':''}"), BooleanBean.class);
        assertNotNull(b.value);
        assertFalse(b.value.isPresent());
    }
}
