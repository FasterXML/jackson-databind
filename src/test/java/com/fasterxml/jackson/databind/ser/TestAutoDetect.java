package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

/**
 * Unit tests for checking extended auto-detect configuration,
 * in context of serialization
 */
public class TestAutoDetect
    extends BaseMapTest
{
    static class FieldBean
    {
        public String p1 = "public";
        protected String p2 = "protected";
        @SuppressWarnings("unused")
        private String p3 = "private";
    }

    @JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
    static class ProtFieldBean extends FieldBean { }

    static class MethodBean
    {
        public String getA() { return "a"; }
        protected String getB() { return "b"; }
        @SuppressWarnings("unused")
        private String getC() { return "c"; }
    }

    @JsonAutoDetect(getterVisibility=JsonAutoDetect.Visibility.PROTECTED_AND_PUBLIC)
    static class ProtMethodBean extends MethodBean { }

    /*
    /*********************************************************
    /* Test methods
    /*********************************************************
     */

    public void testDefaults() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // by default, only public fields and getters are detected
        assertEquals("{\"p1\":\"public\"}",
                     m.writeValueAsString(new FieldBean()));
        assertEquals("{\"a\":\"a\"}",
                     m.writeValueAsString(new MethodBean()));
    }

    public void testProtectedViaAnnotations() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        Map<String,Object> result = writeAndMap(m, new ProtFieldBean());
        assertEquals(2, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertNull(result.get("p3"));

        result = writeAndMap(m, new ProtMethodBean());
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertNull(result.get("c"));
    }

    public void testPrivateUsingGlobals() throws Exception
    {
        ObjectMapper m = ObjectMapper.builder()
                .changeDefaultVisibility(vc ->
                    vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                .build();
        
        Map<String,Object> result = writeAndMap(m, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));

        m = ObjectMapper.builder()
                .changeDefaultVisibility(vc ->
                    vc.withGetterVisibility(JsonAutoDetect.Visibility.ANY)
                    )
                .build();
        result = writeAndMap(m, new MethodBean());
        assertEquals(3, result.size());
        assertEquals("a", result.get("a"));
        assertEquals("b", result.get("b"));
        assertEquals("c", result.get("c"));
    }

    // [JACKSON-621]
    public void testBasicSetup() throws Exception
    {
        ObjectMapper mapper = ObjectMapper.builder()
                .changeDefaultVisibility(vc ->
                    vc.with(JsonAutoDetect.Visibility.ANY))
                .build();
        Map<String,Object> result = writeAndMap(mapper, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));
    }

    // [JACKSON-595]
    public void testMapperShortcutMethods() throws Exception
    {
        ObjectMapper mapper = ObjectMapper.builder()
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY))
                .build();

        Map<String,Object> result = writeAndMap(mapper, new FieldBean());
        assertEquals(3, result.size());
        assertEquals("public", result.get("p1"));
        assertEquals("protected", result.get("p2"));
        assertEquals("private", result.get("p3"));
    }

}
