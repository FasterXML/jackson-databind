package com.fasterxml.jackson.databind.deser;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.JDKScalarsTest.PrimitivesBean;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class NullHandlingTest extends BaseMapTest
{
    static class FunnyNullDeserializer extends JsonDeserializer<String>
    {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return "text";
        }

        @Override
        public String getNullValue(DeserializationContext ctxt) { return "funny"; }
    }

    static class AnySetter{

        private Map<String,String> any = new HashMap<String,String>();

        @JsonAnySetter
        public void setAny(String name, String value){
            this.any.put(name,value);
        }

        public Map<String,String> getAny(){
            return this.any;
        }
    }

    private final ObjectMapper MAPPER = objectMapper();
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testNull() throws Exception
    {
        // null doesn't really have a type, fake by assuming Object
        Object result = MAPPER.readValue("   null", Object.class);
        assertNull(result);
    }  
    
    public void testAnySetterNulls() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        String fieldName = "fieldName";
        String nullValue = "{\""+fieldName+"\":null}";

        // should get non-default null directly:
        AnySetter result = mapper.readValue(nullValue, AnySetter.class);

        assertEquals(1, result.getAny().size());
        assertNotNull(result.getAny().get(fieldName));
        assertEquals("funny", result.getAny().get(fieldName));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(AnySetter.class);
        result = reader.readValue(nullValue);

        assertEquals(1, result.getAny().size());
        assertNotNull(result.getAny().get(fieldName));
        assertEquals("funny", result.getAny().get(fieldName));
    }

    public void testCustomRootNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        // should get non-default null directly:
        String str = mapper.readValue("null", String.class);
        assertNotNull(str);
        assertEquals("funny", str);
        
        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(String.class);
        str = reader.readValue("null");
        assertNotNull(str);
        assertEquals("funny", str);
    }

    public void testNullForPrimitives() throws IOException
    {
        // by default, ok to rely on defaults
        PrimitivesBean bean = MAPPER.readValue(
                "{\"intValue\":null, \"booleanValue\":null, \"doubleValue\":null}",
                PrimitivesBean.class);
        assertNotNull(bean);
        assertEquals(0, bean.intValue);
        assertEquals(false, bean.booleanValue);
        assertEquals(0.0, bean.doubleValue);

        bean = MAPPER.readValue("{\"byteValue\":null, \"longValue\":null, \"floatValue\":null}",
                PrimitivesBean.class);
        assertNotNull(bean);
        assertEquals((byte) 0, bean.byteValue);
        assertEquals(0L, bean.longValue);
        assertEquals(0.0f, bean.floatValue);
        
        // but not when enabled
        final ObjectReader reader = MAPPER
                .readerFor(PrimitivesBean.class)
                .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        // boolean
        try {
            reader.readValue("{\"booleanValue\":null}");
            fail("Expected failure for boolean + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type boolean");
        }
        // byte/char/short/int/long
        try {
            reader.readValue("{\"byteValue\":null}");
            fail("Expected failure for byte + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type byte");
        }
        try {
            reader.readValue("{\"charValue\":null}");
            fail("Expected failure for char + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type char");
        }
        try {
            reader.readValue("{\"shortValue\":null}");
            fail("Expected failure for short + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type short");
        }
        try {
            reader.readValue("{\"intValue\":null}");
            fail("Expected failure for int + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type int");
        }
        try {
            reader.readValue("{\"longValue\":null}");
            fail("Expected failure for long + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type long");
        }

        // float/double
        try {
            reader.readValue("{\"floatValue\":null}");
            fail("Expected failure for float + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type float");
        }
        try {
            reader.readValue("{\"doubleValue\":null}");
            fail("Expected failure for double + null");
        } catch (JsonMappingException e) {
            verifyException(e, "Can not map JSON null into type double");
        }
    }
    
    // [databind#407]
    public void testListOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        List<String> list = Arrays.asList("funny");
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, String.class);

        // should get non-default null directly:
        List<?> deser = mapper.readValue("[null]", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(type);
        deser = reader.readValue("[null]");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals(list.get(0), deser.get(0));
    }

    // Test for [#407]
    public void testMapOfNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        JavaType type = mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class);
        // should get non-default null directly:
        Map<?,?> deser = mapper.readValue("{\"key\":null}", type);
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));

        // as well as via ObjectReader
        ObjectReader reader = mapper.readerFor(type);
        deser = reader.readValue("{\"key\":null}");
        assertNotNull(deser);
        assertEquals(1, deser.size());
        assertEquals("funny", deser.get("key"));
    }
}
