package com.fasterxml.jackson.databind.format;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

// [databind#1480]
public class BooleanFormatTest extends BaseMapTest
{
    @JsonPropertyOrder({ "b1", "b2", "b3" })
    static class BeanWithBoolean
    {
        @JsonFormat(shape=JsonFormat.Shape.NUMBER)
        public boolean b1;

        @JsonFormat(shape=JsonFormat.Shape.NUMBER)
        public Boolean b2;

        public boolean b3;

        public BeanWithBoolean() { }
        public BeanWithBoolean(boolean b1, Boolean b2, boolean b3) {
            this.b1 = b1;
            this.b2 = b2;
            this.b3 = b3;
        }
    }

    /**
     * Simple wrapper around boolean types, usually to test value
     * conversions or wrapping
     */
    protected static class BooleanWrapper {
        public Boolean b;

        public BooleanWrapper() { }
        public BooleanWrapper(Boolean value) { b = value; }
    }

    // [databind#3080]
    protected static class PrimitiveBooleanWrapper {
        public boolean b;

        public PrimitiveBooleanWrapper() { }
        public PrimitiveBooleanWrapper(boolean value) { b = value; }
    }

    static class AltBoolean extends BooleanWrapper
    {
        public AltBoolean() { }
        public AltBoolean(Boolean b) { super(b); }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final static ObjectMapper MAPPER = newJsonMapper();

    public void testShapeViaDefaults() throws Exception
    {
        assertEquals(a2q("{'b':true}"),
                MAPPER.writeValueAsString(new BooleanWrapper(true)));
        ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(Boolean.class,
                        cfg -> cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER)
        )).build();
        assertEquals(a2q("{'b':1}"),
                m.writeValueAsString(new BooleanWrapper(true)));

        m = jsonMapperBuilder()
                .withConfigOverride(Boolean.class,
                        cfg -> cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING)
        )).build();
        assertEquals(a2q("{'b':'true'}"),
                m.writeValueAsString(new BooleanWrapper(true)));
    }

    // [databind#3080]
    public void testPrimitiveShapeViaDefaults() throws Exception
    {
        assertEquals(a2q("{'b':true}"),
                MAPPER.writeValueAsString(new PrimitiveBooleanWrapper(true)));
        ObjectMapper m = jsonMapperBuilder()
                .withConfigOverride(Boolean.TYPE, cfg ->
                    cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.NUMBER))
        ).build();
        assertEquals(a2q("{'b':1}"),
                m.writeValueAsString(new PrimitiveBooleanWrapper(true)));

        m = jsonMapperBuilder()
                .withConfigOverride(Boolean.TYPE, cfg ->
                    cfg.setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING))
        ).build();
        assertEquals(a2q("{'b':'true'}"),
                m.writeValueAsString(new PrimitiveBooleanWrapper(true)));
    }

    public void testShapeOnProperty() throws Exception
    {
        assertEquals(a2q("{'b1':1,'b2':0,'b3':true}"),
                MAPPER.writeValueAsString(new BeanWithBoolean(true, false, true)));
    }
}
