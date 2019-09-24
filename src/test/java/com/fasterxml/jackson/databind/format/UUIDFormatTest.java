package com.fasterxml.jackson.databind.format;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UUIDFormatTest extends BaseMapTest
{
    protected static class UUIDWrapper {
        @JsonFormat(shape=JsonFormat.Shape.NATURAL)
        public UUID u;

        public UUIDWrapper() { }
        public UUIDWrapper(UUID value) { u = value; }
    }

    protected static class UUIDAsString {
        @JsonFormat(shape=JsonFormat.Shape.STRING)
        public UUID u;

        public UUIDAsString() { }
        public UUIDAsString(UUID value) { u = value; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

//    private final static ObjectMapper MAPPER = newJsonMapper();
    private final static ObjectMapper MAPPER = new ObjectMapper();

    public void testShapeViaDefaults() throws Exception
    {
/*
        assertEquals(aposToQuotes("{'b':true}"),
                MAPPER.writeValueAsString(new BooleanWrapper(true)));
        ObjectMapper m = newJsonMapper();
        m.configOverride(Boolean.class)
            .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER));
        assertEquals(aposToQuotes("{'b':1}"),
                m.writeValueAsString(new BooleanWrapper(true)));
*/
    }

    /*
    public void testShapeOnProperty() throws Exception
    {
        assertEquals(aposToQuotes("{'b1':1,'b2':0,'b3':true}"),
                MAPPER.writeValueAsString(new BeanWithBoolean(true, false, true)));
    }
    */
    
}
