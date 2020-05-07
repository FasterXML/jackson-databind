package com.fasterxml.jackson.databind.deser.jdk;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

// [databind#2675]: Void-valued "properties"
public class VoidProperties2675Test extends BaseMapTest
{
    static class VoidBean {
        protected Void value;

        public Void getValue() { return null; }

//        public void setValue(Void v) { }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper DEFAULT_MAPPER = sharedMapper();

    private final ObjectMapper VOID_MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.ALLOW_VOID_VALUED_PROPERTIES)
            .build();

    public void testVoidBeanSerialization() throws Exception
    {
        // By default (2.x), not enabled:
        try {
            DEFAULT_MAPPER.writeValueAsString(new VoidBean());
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no properties discovered");
        }

        // but when enabled
        assertEquals("{\"value\":null}", VOID_MAPPER.writeValueAsString(new VoidBean()));
    }

    public void testVoidBeanDeserialization() throws Exception {
        final String DOC = "{\"value\":null}";
        VoidBean result;

        // By default (2.x), not enabled:
        try {
            result = DEFAULT_MAPPER.readValue(DOC, VoidBean.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized field \"value\"");
        }

        // but when enabled
        result = VOID_MAPPER.readValue(DOC, VoidBean.class);
        assertNotNull(result);
        assertNull(result.getValue());
    }
}
